#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#include <pthread.h>
#include <stdint.h>
#include <dlfcn.h>
#include <signal.h>
#include <link.h>

#define LOG_TAG "SF2IAP"
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/*
 * Shadow Fight 2 - IAP Bypass v41
 *
 * Strategy: Don't use IL2CPP API at JNI_OnLoad time — runtime isn't init'd yet.
 * 1. Hook GooglePlayStore.Purchase entry ONLY (blocks Google Play, no API needed)
 * 2. On first purchase trigger → NOW init IL2CPP API (runtime is ready by then)
 * 3. Find methods, rewrite VP/CP, trigger OnPurchaseSucceeded
 */

#define PURCHASE_RVA 0x3C6368C
#define ENTRY_SIZE 16

static void write_log(const char* msg) {
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v41.txt", "a");
    if (fp) { fprintf(fp, "%s\n", msg); fflush(fp); fclose(fp); }
}

static void write_crash(int sig, siginfo_t* info, void* ctx) {
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v41-crash.txt", "a");
    if (fp) {
        fprintf(fp, "=== CRASH sig=%d fault=%p ===\n", sig, info->si_addr);
        fflush(fp); fsync(fileno(fp)); fclose(fp);
    }
    _exit(1);
}

static uintptr_t found_base = 0;

static int dl_callback(struct dl_phdr_info* info, size_t size, void* data) {
    if (!info->dlpi_name) return 0;
    const char* name = info->dlpi_name;
    const char* last_slash = strrchr(name, '/');
    const char* libname = last_slash ? last_slash + 1 : name;
    if (strstr(libname, "libil2cpp.so")) {
        found_base = (uintptr_t)info->dlpi_addr;
        char buf[512];
        snprintf(buf, sizeof(buf), "dl_iterate: found libil2cpp base=0x%lx", (long)found_base);
        write_log(buf);
        return 1;
    }
    return 0;
}

static uintptr_t find_libil2cpp(void) {
    found_base = 0;
    dl_iterate_phdr(dl_callback, NULL);
    return found_base;
}

static int make_writable(void* addr, size_t len) {
    long page = sysconf(_SC_PAGESIZE);
    void* start = (void*)((uintptr_t)addr & ~(page - 1));
    size_t alloc_len = ((len + page - 1) / page) * page + page;
    return mprotect(start, alloc_len, PROT_READ | PROT_WRITE | PROT_EXEC) == 0;
}

static void flush_icache(void* addr, size_t len) {
    uintptr_t a = (uintptr_t)addr & ~15UL;
    for (uintptr_t i = a; i < (uintptr_t)addr + len; i += 16) {
        __asm__ volatile("dc cvau, %0" :: "r"(i));
        __asm__ volatile("ic ivau, %0" :: "r"(i));
    }
    __asm__ volatile("dsb ish");
    __asm__ volatile("isb");
}

/* ==== IL2CPP API (loaded on-demand) ==== */
typedef void* Il2CppDomain;
typedef void* Il2CppAssembly;
typedef void* Il2CppImage;
typedef void* Il2CppClass;
typedef void* Il2CppMethod;
typedef void* Il2CppObject;
typedef void* Il2CppString;
typedef void* Il2CppField;

static Il2CppDomain (*fp_domain_get)(void);
static const Il2CppAssembly** (*fp_domain_get_assemblies)(const Il2CppDomain*, size_t*);
static Il2CppImage* (*fp_assembly_get_image)(const Il2CppAssembly*);
static Il2CppClass* (*fp_class_from_name)(const Il2CppImage*, const char*, const char*);
static Il2CppMethod* (*fp_class_get_method_from_name)(Il2CppClass*, const char*, int);
static Il2CppObject* (*fp_object_new)(const Il2CppClass*);
static Il2CppString* (*fp_string_new)(const char*);
static void (*fp_field_set_value)(Il2CppObject*, Il2CppField*, void*);

static int il2cpp_api_ready = 0;

static int load_il2cpp_api(void) {
    char buf[256];
    int ok = 1;

    void* handle = dlopen("libil2cpp.so", RTLD_NOW | RTLD_NOLOAD);
    if (!handle) {
        write_log("ERROR: dlopen failed in load_api");
        return 0;
    }

    #define TRY(var, sym) fp_##var = (typeof(fp_##var))dlsym(handle, sym); \
        snprintf(buf, sizeof(buf), "dlsym(%s) = %p", sym, fp_##var); \
        write_log(buf); \
        if (!fp_##var) { ok = 0; }

    TRY(domain_get,            "il2cpp_domain_get")
    TRY(domain_get_assemblies, "il2cpp_domain_get_assemblies")
    TRY(assembly_get_image,    "il2cpp_assembly_get_image")
    TRY(class_from_name,       "il2cpp_class_from_name")
    TRY(class_get_method_from_name, "il2cpp_class_get_method_from_name")
    TRY(object_new,            "il2cpp_object_new")
    TRY(string_new,            "il2cpp_string_new")
    TRY(field_set_value,       "il2cpp_field_set_value")
    #undef TRY

    if (ok) {
        il2cpp_api_ready = 1;
        write_log("IL2CPP API loaded OK");
    } else {
        write_log("IL2CPP API load FAILED");
    }
    return ok;
}

/* ==== Hook ==== */
static uint8_t TRAMP[32] __attribute__((aligned(16)));
static uintptr_t hook_target = 0;

static void install_hook(uintptr_t target, const char* name) {
    memcpy(TRAMP, (void*)target, ENTRY_SIZE);
    TRAMP[16] = 0x50; TRAMP[17] = 0x00; TRAMP[18] = 0x00; TRAMP[19] = 0x58;
    TRAMP[20] = 0x00; TRAMP[21] = 0x02; TRAMP[22] = 0x1F; TRAMP[23] = 0xD6;
    uintptr_t cont = target + ENTRY_SIZE;
    memcpy(TRAMP + 24, &cont, 8);

    if (!make_writable((void*)target, 32)) {
        write_log("ERROR: mprotect failed");
        return;
    }
    uint8_t entry[ENTRY_SIZE];
    entry[0] = 0x50; entry[1] = 0x00; entry[2] = 0x00; entry[3] = 0x58;
    entry[4] = 0x00; entry[5] = 0x02; entry[6] = 0x1F; entry[7] = 0xD6;
    uintptr_t t = (uintptr_t)TRAMP;
    memcpy(entry + 8, &t, 8);
    memcpy((void*)target, entry, ENTRY_SIZE);
    flush_icache((void*)target, ENTRY_SIZE);

    hook_target = target;
    char buf[128];
    snprintf(buf, sizeof(buf), "Hook OK: %s @ 0x%lx", name, (long)target);
    write_log(buf);
}

/* ==== Cached refs (set on first purchase) ==== */
static Il2CppMethod* m_OnPurchaseSucceeded = 0;
static int methods_ready = 0;

static void ensure_methods_ready(void) {
    if (methods_ready) return;

    write_log("ensure_methods: loading IL2CPP API...");
    if (!load_il2cpp_api()) {
        write_log("ERROR: load_il2cpp_api failed");
        return;
    }

    write_log("ensure_methods: calling domain_get...");
    Il2CppDomain* domain = fp_domain_get();
    if (!domain) {
        write_log("ERROR: domain_get returned NULL (runtime not init?)");
        return;
    }

    size_t count = 0;
    const Il2CppAssembly** assemblies = fp_domain_get_assemblies(domain, &count);
    if (!assemblies || count == 0) {
        write_log("ERROR: no assemblies");
        return;
    }

    char buf[512];
    snprintf(buf, sizeof(buf), "Found %zu assemblies", count);
    write_log(buf);

    for (size_t i = 0; i < count; i++) {
        Il2CppImage* image = fp_assembly_get_image(assemblies[i]);
        if (!image) continue;

        Il2CppClass* klass = fp_class_from_name(image, "UnityEngine.Purchasing", "PurchasingManager");
        if (!klass) continue;

        snprintf(buf, sizeof(buf), "Found PurchasingManager in assembly %zu", i);
        write_log(buf);

        m_OnPurchaseSucceeded = fp_class_get_method_from_name(klass, "OnPurchaseSucceeded", 3);
        if (m_OnPurchaseSucceeded) {
            snprintf(buf, sizeof(buf), "OnPurchaseSucceeded methodPtr=%p", *(void**)m_OnPurchaseSucceeded);
            write_log(buf);
        } else {
            write_log("ERROR: OnPurchaseSucceeded(3) not found");
        }
        break;
    }

    if (!m_OnPurchaseSucceeded) {
        write_log("ERROR: PurchasingManager class not found");
    }

    methods_ready = 1;
    write_log("=== methods_ready ===");
}

/* ==== Purchase hook: BLOCK Google Play ==== */
typedef void (*fn_purchase)(uint64_t, uint64_t, uint64_t);

static void hooked_purchase(uint64_t x0, uint64_t x1, uint64_t x2) {
    write_log(">>> PURCHASE INTERCEPTED");

    ensure_methods_ready();

    if (!m_OnPurchaseSucceeded) {
        write_log("ERROR: OnPurchaseSucceeded not available, blocking anyway");
        return;
    }

    /* Extract product ID */
    void* product_id_ptr = *(void**)(x1 + 0x18);
    char product_id[256] = "unknown";
    if (product_id_ptr) {
        int len = *(int*)(product_id_ptr + 0x10);
        if (len > 0 && len < 128) {
            uint16_t* chars = (uint16_t*)(product_id_ptr + 0x14);
            for (int i = 0; i < len && i < 255; i++) {
                product_id[i] = (char)chars[i];
            }
            product_id[len] = 0;
        }
    }

    char buf[512];
    snprintf(buf, sizeof(buf), "Product: %s", product_id);
    write_log(buf);

    /* Get PurchasingManager from GooglePlayStore + 0x30 -> + 0x10 */
    void* gp_cb = *(void**)(x0 + 0x30);
    if (!gp_cb) { write_log("ERROR: GooglePlayPurchaseCallback is NULL"); return; }
    void* mgr = *(void**)(gp_cb + 0x10);
    if (!mgr) { write_log("ERROR: PurchasingManager is NULL"); return; }

    void* receipt = fp_string_new("{}");
    void* tx_id = fp_string_new("fake_tx_001");

    typedef void (*fn_onsuccess)(void* this, void* id, void* receipt, void* tx);
    fn_onsuccess fn = (fn_onsuccess)(*(void**)m_OnPurchaseSucceeded);

    snprintf(buf, sizeof(buf), "Calling OnPurchaseSucceeded(%s) mgr=%p fn=%p", product_id, mgr, fn);
    write_log(buf);

    fn(mgr, product_id_ptr, receipt, tx_id);
    write_log("OnPurchaseSucceeded returned OK");
}

/* ==== Main ==== */
static void* hook_thread(void* arg) {
    write_log("=== SF2 IAP Bypass v41 ===");

    struct sigaction sa;
    sa.sa_sigaction = write_crash;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGBUS, &sa, NULL);
    sigaction(SIGABRT, &sa, NULL);

    uintptr_t il2cpp_base = 0;
    for (int i = 0; i < 60; i++) {
        il2cpp_base = find_libil2cpp();
        if (il2cpp_base) break;
        usleep(500000);
    }

    if (!il2cpp_base) {
        write_log("ERROR: libil2cpp.so not found after 30s");
        return NULL;
    }

    char buf[256];
    snprintf(buf, sizeof(buf), "libil2cpp base=0x%lx", (long)il2cpp_base);
    write_log(buf);

    install_hook(il2cpp_base + PURCHASE_RVA, "GooglePlayStore.Purchase");

    write_log("=== Hook installed, waiting for first purchase ===");
    return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    write_log("=== JNI_OnLoad v41 ===");
    pthread_t tid;
    pthread_create(&tid, NULL, hook_thread, NULL);
    pthread_detach(tid);
    return JNI_VERSION_1_6;
}
