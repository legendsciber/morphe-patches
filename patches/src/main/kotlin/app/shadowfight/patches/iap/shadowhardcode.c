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

/*
 * Shadow Fight 2 - IAP Bypass v42
 *
 * Method pointer rewrite. Single sigsetjmp for entire init.
 * NO per-call sigsetjmp (crashes fprintf state).
 */

static void write_log(const char* msg) {
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v42.txt", "a");
    if (fp) { fprintf(fp, "%s\n", msg); fflush(fp); fclose(fp); }
}

/* ==== IL2CPP API ==== */
typedef void* Il2CppDomain;
typedef void* Il2CppAssembly;
typedef void* Il2CppImage;
typedef void* Il2CppClass;
typedef void* Il2CppMethod;
typedef void* Il2CppString;

static Il2CppDomain (*fp_domain_get)(void);
static const Il2CppAssembly** (*fp_domain_get_assemblies)(const Il2CppDomain*, size_t*);
static Il2CppImage* (*fp_assembly_get_image)(const Il2CppAssembly*);
static Il2CppClass* (*fp_class_from_name)(const Il2CppImage*, const char*, const char*);
static Il2CppMethod* (*fp_class_get_method_from_name)(Il2CppClass*, const char*, int);
static Il2CppString* (*fp_string_new)(const char*);

static int load_api(void* h) {
    int ok = 1;
    #define L(sym, var) fp_##var = dlsym(h, #sym); if(!fp_##var) ok=0;
    L(il2cpp_domain_get, domain_get);
    L(il2cpp_domain_get_assemblies, domain_get_assemblies);
    L(il2cpp_assembly_get_image, assembly_get_image);
    L(il2cpp_class_from_name, class_from_name);
    L(il2cpp_class_get_method_from_name, class_get_method_from_name);
    L(il2cpp_string_new, string_new);
    #undef L
    return ok;
}

/* ==== Hook function ==== */
static Il2CppMethod* m_OnPurchaseSucceeded = 0;

void hooked_purchase_entry(void* this_ptr, void* product_def, void* price_override) {
    write_log(">>> PURCHASE INTERCEPTED");

    if (!m_OnPurchaseSucceeded || !fp_string_new) {
        write_log("ERROR: not ready");
        return;
    }

    void* product_id_ptr = product_def ? *(void**)((uintptr_t)product_def + 0x18) : NULL;
    char product_id[256] = "unknown";
    if (product_id_ptr) {
        int len = *(int*)((uintptr_t)product_id_ptr + 0x10);
        if (len > 0 && len < 128) {
            uint16_t* chars = (uint16_t*)((uintptr_t)product_id_ptr + 0x14);
            for (int i = 0; i < len && i < 255; i++) product_id[i] = (char)chars[i];
            product_id[len] = 0;
        }
    }

    char buf[512];
    snprintf(buf, sizeof(buf), "Product: %s", product_id);
    write_log(buf);

    void* gp_cb = *(void**)((uintptr_t)this_ptr + 0x30);
    void* mgr = gp_cb ? *(void**)((uintptr_t)gp_cb + 0x10) : NULL;
    if (!mgr) { write_log("ERROR: PurchasingManager NULL"); return; }

    void* receipt = fp_string_new("{}");
    void* tx_id = fp_string_new("fake_tx_001");

    typedef void (*fn_t)(void*, void*, void*, void*);
    fn_t fn = (fn_t)(*(void**)m_OnPurchaseSucceeded);

    snprintf(buf, sizeof(buf), "Calling OnPurchaseSucceeded mgr=%p", mgr);
    write_log(buf);

    fn(mgr, product_id_ptr, receipt, tx_id);
    write_log("OnPurchaseSucceeded OK");
}

/* ==== Init: single crash guard for entire process ==== */
static sigjmp_buf g_jmp;
static volatile int g_crashed = 0;

static void on_crash(int sig, siginfo_t* info, void* ctx) {
    g_crashed = 1;
    siglongjmp(g_jmp, 1);
}

/* Safe wrappers that use g_crashed flag */
static Il2CppDomain* safe_domain_get(void) {
    g_crashed = 0;
    return fp_domain_get();
}

static const Il2CppAssembly** safe_get_assemblies(const Il2CppDomain* d, size_t* c) {
    g_crashed = 0;
    return fp_domain_get_assemblies(d, c);
}

static void* safe_image(const Il2CppAssembly* a) {
    g_crashed = 0;
    return fp_assembly_get_image(a);
}

static void* safe_class(void* img, const char* ns, const char* name) {
    g_crashed = 0;
    return fp_class_from_name(img, ns, name);
}

static void* safe_method(void* klass, const char* name, int argc) {
    g_crashed = 0;
    return fp_class_get_method_from_name(klass, name, argc);
}

static void* init_thread(void* arg) {
    write_log("=== SF2 IAP Bypass v42 ===");

    /* Install crash handler ONCE for the entire init */
    struct sigaction sa, old_segv, old_abrt;
    sa.sa_sigaction = on_crash;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, &old_segv);
    sigaction(SIGABRT, &sa, &old_abrt);

    /* Wait for libil2cpp.so */
    void* handle = NULL;
    for (int i = 0; i < 120 && !handle; i++) {
        handle = dlopen("libil2cpp.so", RTLD_NOW | RTLD_NOLOAD);
        if (!handle) usleep(250000);
    }
    if (!handle) { write_log("ERROR: no libil2cpp"); return NULL; }
    write_log("libil2cpp found");

    if (!load_api(handle)) { write_log("ERROR: API load"); return NULL; }
    write_log("API loaded");

    /* Main init loop with crash recovery */
    for (int attempt = 0; attempt < 120; attempt++) {
        char buf[256];

        if (sigsetjmp(g_jmp, 1) != 0) {
            /* Crashed — log and retry */
            snprintf(buf, sizeof(buf), "Crash in attempt %d, retrying...", attempt);
            write_log(buf);
            usleep(1000000);
            continue;
        }

        snprintf(buf, sizeof(buf), "Attempt %d", attempt);
        write_log(buf);

        Il2CppDomain* domain = safe_domain_get();
        if (!domain) { usleep(500000); continue; }

        size_t count = 0;
        const Il2CppAssembly** asms = safe_get_assemblies(domain, &count);
        if (!asms || count == 0) { usleep(500000); continue; }

        snprintf(buf, sizeof(buf), "Got %zu assemblies", count);
        write_log(buf);

        /* Find GooglePlayStore.Purchase */
        Il2CppMethod* purchase_method = NULL;
        for (size_t i = 0; i < count; i++) {
            void* img = safe_image(asms[i]);
            if (!img) continue;
            void* klass = safe_class(img, "UnityEngine.Purchasing", "GooglePlayStore");
            if (!klass) continue;
            snprintf(buf, sizeof(buf), "GooglePlayStore found in asm %zu", i);
            write_log(buf);
            purchase_method = safe_method(klass, "Purchase", 2);
            if (purchase_method) {
                snprintf(buf, sizeof(buf), "Purchase method=%p ptr=%p", purchase_method, *(void**)purchase_method);
                write_log(buf);
            }
            break;
        }

        /* Find PurchasingManager.OnPurchaseSucceeded */
        for (size_t i = 0; i < count; i++) {
            void* img = safe_image(asms[i]);
            if (!img) continue;
            void* klass = safe_class(img, "UnityEngine.Purchasing", "PurchasingManager");
            if (!klass) continue;
            m_OnPurchaseSucceeded = safe_method(klass, "OnPurchaseSucceeded", 3);
            if (m_OnPurchaseSucceeded) {
                snprintf(buf, sizeof(buf), "OnPurchaseSucceeded ptr=%p", *(void**)m_OnPurchaseSucceeded);
                write_log(buf);
            }
            break;
        }

        if (!purchase_method || !m_OnPurchaseSucceeded) {
            write_log("Methods not found yet");
            usleep(500000);
            continue;
        }

        /* REWRITE METHOD POINTER */
        void** slot = (void**)purchase_method;
        snprintf(buf, sizeof(buf), "Rewrite: %p -> %p", slot[0], (void*)hooked_purchase_entry);
        write_log(buf);

        long page = sysconf(_SC_PAGESIZE);
        void* start = (void*)((uintptr_t)slot & ~(page - 1));
        if (mprotect(start, 2 * page, PROT_READ | PROT_WRITE) == 0) {
            slot[0] = (void*)hooked_purchase_entry;
            write_log("=== METHOD POINTER REWRITTEN ===");
        } else {
            write_log("ERROR: mprotect");
        }

        /* Restore original signal handlers */
        sigaction(SIGSEGV, &old_segv, NULL);
        sigaction(SIGABRT, &old_abrt, NULL);
        return NULL;
    }

    write_log("ERROR: init failed after all attempts");
    sigaction(SIGSEGV, &old_segv, NULL);
    sigaction(SIGABRT, &old_abrt, NULL);
    return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    write_log("=== JNI_OnLoad v42 ===");
    pthread_t tid;
    pthread_create(&tid, NULL, init_thread, NULL);
    pthread_detach(tid);
    return JNI_VERSION_1_6;
}
