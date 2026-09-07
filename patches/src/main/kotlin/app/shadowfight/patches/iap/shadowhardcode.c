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
#include <setjmp.h>
#include <link.h>

#define LOG_TAG "SF2IAP"
#include <android/log.h>
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/*
 * Shadow Fight 2 - IAP Bypass v42
 *
 * NO ENTRY HOOKS (trampoline crashes on PC-relative instructions).
 * Strategy:
 * 1. Poll until libil2cpp.so loaded + il2cpp_init called
 * 2. Use IL2CPP API to find GooglePlayStore.Purchase MethodInfo
 * 3. Rewrite methodPointer to our hook
 * 4. Our hook blocks Google Play + calls OnPurchaseSucceeded
 */

static volatile int g_log_in_progress = 0;

static void write_log(const char* msg) {
    if (g_log_in_progress) return;
    g_log_in_progress = 1;
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v42.txt", "a");
    if (fp) { fprintf(fp, "%s\n", msg); fflush(fp); fclose(fp); }
    g_log_in_progress = 0;
}

static void write_crash(int sig, siginfo_t* info, void* ctx) {
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v42-crash.txt", "a");
    if (fp) {
        fprintf(fp, "=== CRASH sig=%d fault=%p ===\n", sig, info->si_addr);
        fflush(fp); fsync(fileno(fp)); fclose(fp);
    }
}

/* ==== IL2CPP API ==== */
typedef void* Il2CppDomain;
typedef void* Il2CppAssembly;
typedef void* Il2CppImage;
typedef void* Il2CppClass;
typedef void* Il2CppMethod;
typedef void* Il2CppObject;
typedef void* Il2CppString;

static Il2CppDomain (*fp_domain_get)(void);
static const Il2CppAssembly** (*fp_domain_get_assemblies)(const Il2CppDomain*, size_t*);
static Il2CppImage* (*fp_assembly_get_image)(const Il2CppAssembly*);
static Il2CppClass* (*fp_class_from_name)(const Il2CppImage*, const char*, const char*);
static Il2CppMethod* (*fp_class_get_method_from_name)(Il2CppClass*, const char*, int);
static Il2CppString* (*fp_string_new)(const char*);
static void (*fp_field_set_value)(Il2CppObject*, void*, void*);

static int load_il2cpp_api(void* handle) {
    char buf[256];
    int ok = 1;
    #define TRY(var, sym) fp_##var = (typeof(fp_##var))dlsym(handle, sym); \
        snprintf(buf, sizeof(buf), "dlsym(%s)=%p", sym, fp_##var); \
        write_log(buf); \
        if (!fp_##var) ok = 0;

    TRY(domain_get,            "il2cpp_domain_get")
    TRY(domain_get_assemblies, "il2cpp_domain_get_assemblies")
    TRY(assembly_get_image,    "il2cpp_assembly_get_image")
    TRY(class_from_name,       "il2cpp_class_from_name")
    TRY(class_get_method_from_name, "il2cpp_class_get_method_from_name")
    TRY(string_new,            "il2cpp_string_new")
    TRY(field_set_value,       "il2cpp_field_set_value")
    #undef TRY
    return ok;
}

/* ==== Signal-safe wrapper for domain_get ==== */
static sigjmp_buf g_jmpbuf;
static volatile int g_got_signal = 0;

static void safe_signal(int sig, siginfo_t* info, void* ctx) {
    g_got_signal = sig;
    siglongjmp(g_jmpbuf, 1);
}

static Il2CppDomain* safe_domain_get(void) {
    struct sigaction sa_new, sa_old_segv, sa_old_abrt;
    sa_new.sa_sigaction = safe_signal;
    sa_new.sa_flags = SA_SIGINFO;
    sigemptyset(&sa_new.sa_mask);

    sigaction(SIGSEGV, &sa_new, &sa_old_segv);
    sigaction(SIGABRT, &sa_new, &sa_old_abrt);
    g_got_signal = 0;

    Il2CppDomain* result = NULL;
    if (sigsetjmp(g_jmpbuf, 1) == 0) {
        result = fp_domain_get();
    }

    sigaction(SIGSEGV, &sa_old_segv, NULL);
    sigaction(SIGABRT, &sa_old_abrt, NULL);
    return result;
}

/* ==== Our hook function for GooglePlayStore.Purchase ==== */
/* We'll set this as the methodPointer so when game calls Purchase, it calls us */
/* This function is NOT an entry hook - it replaces the method pointer entirely */

static Il2CppMethod* m_OnPurchaseSucceeded = 0;

/* Il2CppMethod layout: [0x00] = methodPointer */
/* When game calls Purchase(args), our function gets called with same args */
/* For instance methods: x0=this, x1=arg0(ProductDefinition), x2=arg1(string) */

void hooked_purchase_entry(void* this_ptr, void* product_def, void* price_override) {
    write_log(">>> PURCHASE INTERCEPTED via method pointer rewrite");

    if (!m_OnPurchaseSucceeded) {
        write_log("ERROR: OnPurchaseSucceeded not found");
        return;
    }

    /* Extract product ID from ProductDefinition offset 0x18 = storeSpecificId */
    void* product_id_ptr = NULL;
    if (product_def) {
        product_id_ptr = *(void**)((uintptr_t)product_def + 0x18);
    }

    char product_id[256] = "unknown";
    if (product_id_ptr) {
        int len = *(int*)((uintptr_t)product_id_ptr + 0x10);
        if (len > 0 && len < 128) {
            uint16_t* chars = (uint16_t*)((uintptr_t)product_id_ptr + 0x14);
            for (int i = 0; i < len && i < 255; i++) {
                product_id[i] = (char)chars[i];
            }
            product_id[len] = 0;
        }
    }

    char buf[512];
    snprintf(buf, sizeof(buf), "Product: %s this=%p pd=%p", product_id, this_ptr, product_def);
    write_log(buf);

    /* Get PurchasingManager: GooglePlayStore + 0x30 (m_GooglePurchaseCallback) -> + 0x10 (m_StoreCallback) */
    void* gp_cb = *(void**)((uintptr_t)this_ptr + 0x30);
    if (!gp_cb) { write_log("ERROR: GooglePlayPurchaseCallback NULL"); return; }
    void* mgr = *(void**)((uintptr_t)gp_cb + 0x10);
    if (!mgr) { write_log("ERROR: PurchasingManager NULL"); return; }

    void* receipt = fp_string_new("{}");
    void* tx_id = fp_string_new("fake_tx_001");

    typedef void (*fn_onsuccess)(void* this, void* id, void* receipt, void* tx);
    fn_onsuccess fn = (fn_onsuccess)(*(void**)m_OnPurchaseSucceeded);

    snprintf(buf, sizeof(buf), "Calling OnPurchaseSucceeded mgr=%p fn=%p", mgr, fn);
    write_log(buf);

    fn(mgr, product_id_ptr, receipt, tx_id);
    write_log("OnPurchaseSucceeded OK");
}

/* ==== Poll for IL2CPP ready + find & patch method ==== */
static void* init_thread(void* arg) {
    write_log("=== SF2 IAP Bypass v42 ===");

    struct sigaction sa;
    sa.sa_sigaction = write_crash;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGBUS, &sa, NULL);

    void* handle = NULL;
    for (int i = 0; i < 120; i++) {
        handle = dlopen("libil2cpp.so", RTLD_NOW | RTLD_NOLOAD);
        if (handle) {
            char buf[128];
            snprintf(buf, sizeof(buf), "dlopen libil2cpp OK after %d attempts", i);
            write_log(buf);
            break;
        }
        usleep(250000);
    }
    if (!handle) {
        write_log("ERROR: libil2cpp.so not found after 30s");
        return NULL;
    }

    if (!load_il2cpp_api(handle)) {
        write_log("ERROR: API load failed");
        return NULL;
    }

    /* Now poll domain_get with SIGABRT protection */
    Il2CppDomain* domain = NULL;
    for (int i = 0; i < 120; i++) {
        domain = safe_domain_get();
        if (domain) {
            char buf[128];
            snprintf(buf, sizeof(buf), "domain_get OK after %d attempts, domain=%p", i, domain);
            write_log(buf);
            break;
        }
        usleep(250000);
    }
    if (!domain) {
        write_log("ERROR: domain_get failed after 30s");
        return NULL;
    }

    size_t count = 0;
    const Il2CppAssembly** assemblies = NULL;

    /* Poll until assemblies are loaded */
    for (int i = 0; i < 120; i++) {
        count = 0;
        assemblies = fp_domain_get_assemblies(domain, &count);
        if (assemblies && count > 0) {
            char buf2[128];
            snprintf(buf2, sizeof(buf2), "Assemblies ready after %d attempts, count=%zu", i, count);
            write_log(buf2);
            break;
        }
        usleep(250000);
    }

    if (!assemblies || count == 0) {
        write_log("ERROR: assemblies not loaded after 30s");
        return NULL;
    }

    char buf[512];
    snprintf(buf, sizeof(buf), "Found %zu assemblies", count);
    write_log(buf);

    /* Find GooglePlayStore + its Purchase method */
    Il2CppClass* gp_store_klass = NULL;
    Il2CppMethod* purchase_method = NULL;

    for (size_t i = 0; i < count; i++) {
        Il2CppImage* image = fp_assembly_get_image(assemblies[i]);
        if (!image) continue;

        /* Find GooglePlayStore class */
        Il2CppClass* klass = fp_class_from_name(image, "", "GooglePlayStore");
        if (klass) {
            gp_store_klass = klass;
            snprintf(buf, sizeof(buf), "Found GooglePlayStore in assembly %zu", i);
            write_log(buf);

            purchase_method = fp_class_get_method_from_name(klass, "Purchase", 2);
            if (purchase_method) {
                snprintf(buf, sizeof(buf), "Purchase method=%p methodPtr=%p",
                         purchase_method, *(void**)purchase_method);
                write_log(buf);
            }
            break;
        }
    }

    /* Find PurchasingManager.OnPurchaseSucceeded */
    for (size_t i = 0; i < count; i++) {
        Il2CppImage* image = fp_assembly_get_image(assemblies[i]);
        if (!image) continue;

        Il2CppClass* klass = fp_class_from_name(image, "UnityEngine.Purchasing", "PurchasingManager");
        if (klass) {
            m_OnPurchaseSucceeded = fp_class_get_method_from_name(klass, "OnPurchaseSucceeded", 3);
            if (m_OnPurchaseSucceeded) {
                snprintf(buf, sizeof(buf), "OnPurchaseSucceeded methodPtr=%p", *(void**)m_OnPurchaseSucceeded);
                write_log(buf);
            }
            break;
        }
    }

    if (!purchase_method) {
        write_log("ERROR: Purchase method not found");
        return NULL;
    }
    if (!m_OnPurchaseSucceeded) {
        write_log("ERROR: OnPurchaseSucceeded not found");
        return NULL;
    }

    /* REWRITE METHOD POINTER: MethodInfo[0x00] = our function */
    void** method_ptr_slot = (void**)purchase_method;

    char buf2[256];
    snprintf(buf2, sizeof(buf2), "Rewriting Purchase methodPtr: %p -> %p",
             method_ptr_slot[0], (void*)hooked_purchase_entry);
    write_log(buf2);

    /* Make writable and rewrite */
    long page = sysconf(_SC_PAGESIZE);
    void* start = (void*)((uintptr_t)method_ptr_slot & ~(page - 1));
    size_t alloc_len = 2 * page;
    if (mprotect(start, alloc_len, PROT_READ | PROT_WRITE) == 0) {
        method_ptr_slot[0] = (void*)hooked_purchase_entry;
        write_log("=== Purchase method pointer REWRITTEN ===");
    } else {
        write_log("ERROR: mprotect failed for method pointer");
    }

    return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    write_log("=== JNI_OnLoad v42 ===");
    pthread_t tid;
    pthread_create(&tid, NULL, init_thread, NULL);
    pthread_detach(tid);
    return JNI_VERSION_1_6;
}
