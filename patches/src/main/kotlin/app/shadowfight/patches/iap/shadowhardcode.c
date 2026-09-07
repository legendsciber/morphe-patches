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

#define LOG_TAG "SF2IAP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#include <android/log.h>

/*
 * Shadow Fight 2 - IAP Bypass v39
 *
 * Fixed from v38:
 * - Removed broken RVA fallback for OnPurchaseSucceeded MethodInfo
 * - Added diagnostic logging for class/method search
 * - Hook ALWAYS blocks Google Play (never calls original)
 *
 * Flow:
 * 1. Hook GooglePlayStore.Purchase at entry → block Google Play
 * 2. Find PurchasingManager via IL2CPP API → call OnPurchaseSucceeded
 * 3. Rewrite VerifyPurchase/ConfirmPurchase method pointers → immediate success
 */

#define PURCHASE_RVA 0x3C6368C
#define ENTRY_SIZE 16

static void write_log(const char* msg) {
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v39.txt", "a");
    if (fp) { fprintf(fp, "%s\n", msg); fclose(fp); }
}

static void write_crash(int sig, siginfo_t* info, void* ctx) {
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v39-crash.txt", "a");
    if (fp) {
        fprintf(fp, "=== CRASH sig=%d fault=%p ===\n", sig, info->si_addr);
        fflush(fp); fsync(fileno(fp)); fclose(fp);
    }
    _exit(1);
}

static uintptr_t find_libil2cpp_fullpath(char* buf, size_t len) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return 0;
    char line[512];
    uintptr_t base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, "libil2cpp.so")) {
            char* path = strchr(line, '/');
            if (path) {
                char* nl = strchr(path, '\n');
                if (nl) *nl = 0;
                strncpy(buf, path, len);
                sscanf(line, "%lx-", &base);
                break;
            }
        }
    }
    fclose(fp);
    return base;
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

/* ==== IL2CPP API ==== */
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
static void* (*fp_class_get_field_from_name)(Il2CppClass*, const char*);
static Il2CppObject* (*fp_object_new)(const Il2CppClass*);
static Il2CppString* (*fp_string_new)(const char*);
static void (*fp_field_set_value)(Il2CppObject*, Il2CppField*, void*);

static int il2cpp_loaded = 0;

static int load_il2cpp_api(void* handle) {
    #define LOAD(var, sym) fp_##var = (typeof(fp_##var))dlsym(handle, sym); \
        if (!fp_##var) { LOGE("Missing: " sym); return 0; }

    LOAD(domain_get,            "il2cpp_domain_get")
    LOAD(domain_get_assemblies, "il2cpp_domain_get_assemblies")
    LOAD(assembly_get_image,    "il2cpp_assembly_get_image")
    LOAD(class_from_name,       "il2cpp_class_from_name")
    LOAD(class_get_method_from_name, "il2cpp_class_get_method_from_name")
    LOAD(class_get_field_from_name,  "il2cpp_class_get_field_from_name")
    LOAD(object_new,            "il2cpp_object_new")
    LOAD(string_new,            "il2cpp_string_new")
    LOAD(field_set_value,       "il2cpp_field_set_value")

    #undef LOAD
    il2cpp_loaded = 1;
    write_log("IL2CPP API loaded OK");
    return 1;
}

/* ==== Hook ==== */
static uint8_t TRAMP[32] __attribute__((aligned(16)));

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

    char buf[128];
    snprintf(buf, sizeof(buf), "Hook OK: %s @ 0x%lx", name, (long)target);
    write_log(buf);
}

/* ==== Cached refs ==== */
static Il2CppMethod* m_OnPurchaseSucceeded = 0;

static void find_methods(void) {
    Il2CppDomain* domain = fp_domain_get();
    if (!domain) { write_log("ERROR: domain_get failed"); return; }

    size_t count = 0;
    const Il2CppAssembly** assemblies = fp_domain_get_assemblies(domain, &count);
    if (!assemblies || count == 0) { write_log("ERROR: no assemblies"); return; }

    char buf[512];
    snprintf(buf, sizeof(buf), "Found %zu assemblies", count);
    write_log(buf);

    /* Find PurchasingManager.OnPurchaseSucceeded(string, string, string) */
    for (size_t i = 0; i < count; i++) {
        Il2CppImage* image = fp_assembly_get_image(assemblies[i]);
        if (!image) continue;

        Il2CppClass* klass = fp_class_from_name(image, "UnityEngine.Purchasing", "PurchasingManager");
        if (!klass) continue;

        snprintf(buf, sizeof(buf), "Found PurchasingManager in assembly %zu", i);
        write_log(buf);

        m_OnPurchaseSucceeded = fp_class_get_method_from_name(klass, "OnPurchaseSucceeded", 3);
        if (m_OnPurchaseSucceeded) {
            void* ptr = *(void**)m_OnPurchaseSucceeded;
            snprintf(buf, sizeof(buf), "OnPurchaseSucceeded methodPtr=%p", ptr);
            write_log(buf);
        } else {
            write_log("ERROR: OnPurchaseSucceeded(3) not found");
        }
        break;
    }

    if (!m_OnPurchaseSucceeded) {
        write_log("ERROR: PurchasingManager class not found in any assembly");
        /* Log all assembly images for debugging */
        for (size_t i = 0; i < count && i < 10; i++) {
            Il2CppImage* image = fp_assembly_get_image(assemblies[i]);
            if (image) {
                const char* name = ""; /* IL2CPP doesn't expose name getter easily */
                snprintf(buf, sizeof(buf), "Assembly %zu: image=%p", i, image);
                write_log(buf);
            }
        }
    }
}

/* ==== Purchase hook: BLOCK Google Play, trigger game flow ==== */
typedef void (*fn_purchase)(uint64_t, uint64_t, uint64_t);

static void hooked_purchase(uint64_t x0, uint64_t x1, uint64_t x2) {
    write_log(">>> PURCHASE INTERCEPTED - blocking Google Play");

    if (!il2cpp_loaded || !m_OnPurchaseSucceeded) {
        write_log("ERROR: not ready, but still blocking Google Play");
        return; /* DO NOT call original - Google Play is blocked */
    }

    /* Extract product ID from ProductDefinition (offset 0x18 = storeSpecificId) */
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

    /* Get PurchasingManager:
     * GooglePlayStore + 0x30 = GooglePlayPurchaseCallback
     * GooglePlayPurchaseCallback + 0x10 = PurchasingManager (m_StoreCallback)
     */
    void* gp_cb = *(void**)(x0 + 0x30);
    if (!gp_cb) {
        write_log("ERROR: GooglePlayPurchaseCallback is NULL");
        return;
    }

    void* mgr = *(void**)(gp_cb + 0x10);
    if (!mgr) {
        write_log("ERROR: PurchasingManager is NULL");
        return;
    }

    snprintf(buf, sizeof(buf), "PurchasingManager=%p", mgr);
    write_log(buf);

    /* Create fake receipt and tx ID */
    void* receipt = fp_string_new("{}");
    void* tx_id = fp_string_new("fake_tx_001");

    /* Call OnPurchaseSucceeded(id, receipt, txId) */
    typedef void (*fn_onsuccess)(void* this, void* id, void* receipt, void* tx);
    fn_onsuccess fn = (fn_onsuccess)(*(void**)m_OnPurchaseSucceeded);

    snprintf(buf, sizeof(buf), "Calling OnPurchaseSucceeded(%s)", product_id);
    write_log(buf);

    fn(mgr, product_id_ptr, receipt, tx_id);
    write_log("OnPurchaseSucceeded returned OK");
}

/* ==== VerifyPurchase / ConfirmPurchase bypass ==== */
/*
 * Delegate layout (IL2CPP Unity 2021):
 *   0x00: klass
 *   0x08: monitor
 *   0x10: method_ptr
 *   0x18: invoke_impl
 *   0x20: target
 */
static void invoke_callback(void* callback, int success) {
    if (!callback) return;
    void* method_ptr = *(void**)((uintptr_t)callback + 0x10);
    void* target = *(void**)((uintptr_t)callback + 0x20);
    if (!method_ptr) { write_log("ERROR: callback method_ptr NULL"); return; }

    void* ok_str = fp_string_new("OK");
    typedef void (*action_fn)(void* target, int arg1, void* arg2, void* arg3);
    char buf[256];
    snprintf(buf, sizeof(buf), "Callback: ptr=%p target=%p", method_ptr, target);
    write_log(buf);
    ((action_fn)method_ptr)(target, success, ok_str, NULL);
    write_log("Callback done");
}

static void hooked_verify(void* this, void* data, void* cb) {
    write_log(">>> VerifyPurchase bypassed");
    invoke_callback(cb, 1);
}

static void hooked_confirm(void* this, void* data, void* cb) {
    write_log(">>> ConfirmPurchase bypassed");
    invoke_callback(cb, 1);
}

/* ==== Main ==== */
static void* hook_thread(void* arg) {
    write_log("=== SF2 IAP Bypass v39 ===");

    struct sigaction sa;
    sa.sa_sigaction = write_crash;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGBUS, &sa, NULL);
    sigaction(SIGABRT, &sa, NULL);

    char il2cpp_path[256] = {0};
    uintptr_t il2cpp_base = find_libil2cpp_fullpath(il2cpp_path, sizeof(il2cpp_path));
    if (!il2cpp_base) {
        write_log("ERROR: libil2cpp.so not found");
        return NULL;
    }

    char buf[512];
    snprintf(buf, sizeof(buf), "libil2cpp base=0x%lx path=%s", (long)il2cpp_base, il2cpp_path);
    write_log(buf);

    void* handle = dlopen(il2cpp_path, RTLD_NOW);
    if (!handle) {
        snprintf(buf, sizeof(buf), "ERROR: dlopen: %s", dlerror());
        write_log(buf);
        return NULL;
    }

    if (!load_il2cpp_api(handle)) {
        write_log("ERROR: IL2CPP API load failed");
        return NULL;
    }

    find_methods();

    /* Install GooglePlayStore.Purchase hook */
    install_hook(il2cpp_base + PURCHASE_RVA, "GooglePlayStore.Purchase");

    /* Rewrite VerifyPurchase / ConfirmPurchase method pointers */
    Il2CppDomain* domain = fp_domain_get();
    if (domain) {
        size_t count = 0;
        const Il2CppAssembly** assemblies = fp_domain_get_assemblies(domain, &count);
        for (size_t i = 0; i < count; i++) {
            Il2CppImage* image = fp_assembly_get_image(assemblies[i]);
            if (!image) continue;
            Il2CppClass* klass = fp_class_from_name(image, "", "ServerProvider");
            if (!klass) continue;

            snprintf(buf, sizeof(buf), "Found ServerProvider in assembly %zu", i);
            write_log(buf);

            Il2CppMethod* vp = fp_class_get_method_from_name(klass, "VerifyPurchase", 2);
            if (vp) {
                snprintf(buf, sizeof(buf), "VerifyPurchase: orig=%p", *(void**)vp);
                write_log(buf);
                *(void**)vp = hooked_verify;
                write_log("VerifyPurchase rewritten");
            }

            Il2CppMethod* cp = fp_class_get_method_from_name(klass, "ConfirmPurchase", 2);
            if (cp) {
                snprintf(buf, sizeof(buf), "ConfirmPurchase: orig=%p", *(void**)cp);
                write_log(buf);
                *(void**)cp = hooked_confirm;
                write_log("ConfirmPurchase rewritten");
            }
            break;
        }
    }

    write_log("=== All hooks installed ===");
    return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    write_log("=== JNI_OnLoad v39 ===");
    pthread_t tid;
    pthread_create(&tid, NULL, hook_thread, NULL);
    pthread_detach(tid);
    return JNI_VERSION_1_6;
}
