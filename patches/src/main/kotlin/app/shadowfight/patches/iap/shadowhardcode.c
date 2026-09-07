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
 * Method pointer rewrite (no entry hooks).
 * ALL IL2CPP calls wrapped in sigsetjmp crash guard.
 */

static void write_log(const char* msg) {
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v42.txt", "a");
    if (fp) { fprintf(fp, "%s\n", msg); fflush(fp); fclose(fp); }
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
    #undef TRY
    return ok;
}

/* ==== Crash guard: wrap IL2CPP calls in sigsetjmp ==== */
static sigjmp_buf g_jmpbuf;
static volatile int g_crashed = 0;

static void crash_guard(int sig, siginfo_t* info, void* ctx) {
    g_crashed = 1;
    siglongjmp(g_jmpbuf, 1);
}

static struct sigaction g_old_segv, g_old_abrt;

static void install_crash_guard(void) {
    struct sigaction sa;
    sa.sa_sigaction = crash_guard;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, &g_old_segv);
    sigaction(SIGABRT, &sa, &g_old_abrt);
}

static void restore_crash_guard(void) {
    sigaction(SIGSEGV, &g_old_segv, NULL);
    sigaction(SIGABRT, &g_old_abrt, NULL);
}

/* Call func inside crash guard. Returns result or NULL if crashed. */
#define SAFE_CALL(type, func_call) ({ \
    type _result = (type)0; \
    g_crashed = 0; \
    install_crash_guard(); \
    if (sigsetjmp(g_jmpbuf, 1) == 0) { \
        _result = func_call; \
    } else { \
        write_log("SAFE_CALL crashed, retrying later"); \
    } \
    restore_crash_guard(); \
    _result; \
})

/* ==== Our hook function ==== */
static Il2CppMethod* m_OnPurchaseSucceeded = 0;

void hooked_purchase_entry(void* this_ptr, void* product_def, void* price_override) {
    write_log(">>> PURCHASE INTERCEPTED via method pointer rewrite");

    if (!m_OnPurchaseSucceeded) {
        write_log("ERROR: OnPurchaseSucceeded not found");
        return;
    }

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

/* ==== Init thread ==== */
static void* init_thread(void* arg) {
    write_log("=== SF2 IAP Bypass v42 ===");

    struct sigaction sa;
    sa.sa_sigaction = write_crash;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGBUS, &sa, NULL);

    /* Step 1: Wait for libil2cpp.so */
    void* handle = NULL;
    for (int i = 0; i < 120; i++) {
        handle = dlopen("libil2cpp.so", RTLD_NOW | RTLD_NOLOAD);
        if (handle) {
            char buf[128];
            snprintf(buf, sizeof(buf), "dlopen OK after %d attempts", i);
            write_log(buf);
            break;
        }
        usleep(250000);
    }
    if (!handle) { write_log("ERROR: libil2cpp.so not found"); return NULL; }

    if (!load_il2cpp_api(handle)) {
        write_log("ERROR: API load failed");
        return NULL;
    }

    /* Step 2: Poll until ALL IL2CPP calls succeed */
    for (int attempt = 0; attempt < 120; attempt++) {
        char buf[256];
        snprintf(buf, sizeof(buf), "Init attempt %d...", attempt);
        write_log(buf);

        /* domain_get */
        Il2CppDomain* domain = SAFE_CALL(Il2CppDomain*, fp_domain_get());
        if (!domain) {
            usleep(500000);
            continue;
        }

        /* domain_get_assemblies */
        size_t count = 0;
        const Il2CppAssembly** assemblies = SAFE_CALL(const Il2CppAssembly**,
            fp_domain_get_assemblies(domain, &count));
        if (!assemblies || count == 0) {
            snprintf(buf, sizeof(buf), "assemblies not ready (count=%zu), retrying...", count);
            write_log(buf);
            usleep(500000);
            continue;
        }

        snprintf(buf, sizeof(buf), "domain=%p assemblies=%zu — ready!", domain, count);
        write_log(buf);

        /* Step 3: Find GooglePlayStore + Purchase method */
        Il2CppMethod* purchase_method = NULL;

        for (size_t i = 0; i < count; i++) {
            Il2CppImage* image = SAFE_CALL(Il2CppImage*, fp_assembly_get_image(assemblies[i]));
            if (!image) continue;

            Il2CppClass* klass = SAFE_CALL(Il2CppClass*, fp_class_from_name(image, "UnityEngine.Purchasing", "GooglePlayStore"));
            if (klass) {
                snprintf(buf, sizeof(buf), "Found GooglePlayStore in assembly %zu", i);
                write_log(buf);

                purchase_method = SAFE_CALL(Il2CppMethod*,
                    fp_class_get_method_from_name(klass, "Purchase", 2));
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
            Il2CppImage* image = SAFE_CALL(Il2CppImage*, fp_assembly_get_image(assemblies[i]));
            if (!image) continue;

            Il2CppClass* klass = SAFE_CALL(Il2CppClass*,
                fp_class_from_name(image, "UnityEngine.Purchasing", "PurchasingManager"));
            if (klass) {
                m_OnPurchaseSucceeded = SAFE_CALL(Il2CppMethod*,
                    fp_class_get_method_from_name(klass, "OnPurchaseSucceeded", 3));
                if (m_OnPurchaseSucceeded) {
                    snprintf(buf, sizeof(buf), "OnPurchaseSucceeded methodPtr=%p",
                             *(void**)m_OnPurchaseSucceeded);
                    write_log(buf);
                }
                break;
            }
        }

        if (!purchase_method) {
            write_log("ERROR: Purchase method not found");
            usleep(500000);
            continue;
        }
        if (!m_OnPurchaseSucceeded) {
            write_log("ERROR: OnPurchaseSucceeded not found");
            usleep(500000);
            continue;
        }

        /* Step 4: REWRITE METHOD POINTER */
        void** method_ptr_slot = (void**)purchase_method;

        snprintf(buf, sizeof(buf), "Rewriting: %p -> %p",
                 method_ptr_slot[0], (void*)hooked_purchase_entry);
        write_log(buf);

        long page = sysconf(_SC_PAGESIZE);
        void* start = (void*)((uintptr_t)method_ptr_slot & ~(page - 1));
        if (mprotect(start, 2 * page, PROT_READ | PROT_WRITE) == 0) {
            method_ptr_slot[0] = (void*)hooked_purchase_entry;
            write_log("=== Purchase method pointer REWRITTEN ===");
        } else {
            write_log("ERROR: mprotect failed");
        }

        return NULL;
    }

    write_log("ERROR: init failed after all attempts");
    return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    write_log("=== JNI_OnLoad v42 ===");
    pthread_t tid;
    pthread_create(&tid, NULL, init_thread, NULL);
    pthread_detach(tid);
    return JNI_VERSION_1_6;
}
