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
 * Shadow Fight 2 - IAP Bypass v49
 *
 * ELF .dynsym parser — bypasses dlsym entirely.
 * Uses dl_iterate_phdr to find libil2cpp.so, then manually parses
 * the ELF dynamic symbol table to resolve il2cpp_* functions.
 * Creates fake GooglePurchase object and calls OnPurchaseSuccessful on gp_cb.
 */

static volatile int g_log_busy = 0;

static void write_log(const char* msg) {
    if (g_log_busy) return;
    g_log_busy = 1;
    FILE* fp = fopen("/sdcard/Download/sf2-iap-v49.txt", "a");
    if (fp) { fprintf(fp, "%s\n", msg); fflush(fp); fclose(fp); }
    g_log_busy = 0;
}

/* ==== ELF types for manual symbol resolution ==== */
#include <elf.h>

/* Using NDK elf.h for all ELF types */

/* ==== dl_iterate_phdr callback to find libil2cpp.so ==== */
static uintptr_t g_lib_base = 0;
static uintptr_t g_lib_end = 0;

static int find_lib_callback(struct dl_phdr_info* info, size_t size, void* data) {
    if (!info->dlpi_name || !info->dlpi_name[0]) return 0;
    if (strstr(info->dlpi_name, "libil2cpp.so")) {
        g_lib_base = info->dlpi_addr;
        if (info->dlpi_phnum > 0) {
            g_lib_end = info->dlpi_addr + info->dlpi_phdr[info->dlpi_phnum - 1].p_vaddr
                        + info->dlpi_phdr[info->dlpi_phnum - 1].p_memsz;
        }
        char buf[256];
        snprintf(buf, sizeof(buf), "Found libil2cpp at base=%p name=%s",
                 (void*)g_lib_base, info->dlpi_name);
        write_log(buf);
        return 1;
    }
    return 0;
}

/* ==== ELF symbol table search ==== */
static void* find_symbol_in_elf(uintptr_t base, const char* sym_name) {
    Elf64_Ehdr* ehdr = (Elf64_Ehdr*)base;

    /* Validate ELF magic */
    if (memcmp(ehdr, "\x7f""ELF", 4) != 0) {
        write_log("Invalid ELF magic");
        return NULL;
    }

    /* Find section headers */
    Elf64_Shdr* shdr_table = (Elf64_Shdr*)(base + ehdr->e_shoff);
    int sh_count = ehdr->e_shnum;

    /* Find .dynsym and .dynstr */
    Elf64_Shdr* dynsym_sh = NULL;
    Elf64_Shdr* dynstr_sh = NULL;

    for (int i = 0; i < sh_count; i++) {
        if (shdr_table[i].sh_type == SHT_DYNSYM) {
            dynsym_sh = &shdr_table[i];
            /* dynstr is linked section */
            if (shdr_table[i].sh_link < sh_count) {
                dynstr_sh = &shdr_table[shdr_table[i].sh_link];
            }
        }
    }

    if (!dynsym_sh || !dynstr_sh) {
        /* Try PT_DYNAMIC segment approach */
        Elf64_Phdr* phdr = (Elf64_Phdr*)(base + ehdr->e_phoff);
        for (int i = 0; i < ehdr->e_phnum; i++) {
            if (phdr[i].p_type == 2) { /* PT_DYNAMIC */
                /* Parse dynamic section for DT_SYMTAB and DT_STRTAB */
                uint64_t* dyn = (uint64_t*)(base + phdr[i].p_vaddr);
                uint64_t symtab_addr = 0;
                uint64_t strtab_addr = 0;
                uint64_t strtab_size = 0;

                for (int j = 0; dyn[j] != 0; j += 2) {
                    if (dyn[j] == 6) symtab_addr = dyn[j+1];   /* DT_SYMTAB */
                    if (dyn[j] == 5) strtab_addr = dyn[j+1];   /* DT_STRTAB */
                    if (dyn[j] == 10) strtab_size = dyn[j+1];  /* DT_STRSZ */
                }

                if (symtab_addr && strtab_addr) {
                    char* strtab = (char*)(base + strtab_addr);
                    Elf64_Sym* symtab = (Elf64_Sym*)(base + symtab_addr);

                    /* Estimate sym count: use strtab_size as rough upper bound */
                    int max_syms = strtab_size / 24;

                    for (int s = 0; s < max_syms; s++) {
                        if (symtab[s].st_name == 0) continue;
                        if (symtab[s].st_value == 0) continue;
                        const char* name = strtab + symtab[s].st_name;
                        if (strcmp(name, sym_name) == 0) {
                            return (void*)(base + symtab[s].st_value);
                        }
                    }
                }
                break;
            }
        }
        write_log("No .dynsym found via sections, PT_DYNAMIC fallback also failed");
        return NULL;
    }

    char* dynstr = (char*)(base + dynstr_sh->sh_offset);
    Elf64_Sym* dynsym = (Elf64_Sym*)(base + dynsym_sh->sh_offset);
    int sym_count = dynsym_sh->sh_size / sizeof(Elf64_Sym);

    for (int i = 0; i < sym_count; i++) {
        if (dynsym[i].st_name == 0) continue;
        if (dynsym[i].st_value == 0) continue;
        const char* name = dynstr + dynsym[i].st_name;
        if (strcmp(name, sym_name) == 0) {
            return (void*)(base + dynsym[i].st_value);
        }
    }

    return NULL;
}

/* ==== Resolve IL2CPP API via ELF parsing ==== */
typedef void* Il2CppDomain;
typedef void* Il2CppAssembly;
typedef void* Il2CppImage;
typedef void* Il2CppClass;
typedef void* Il2CppMethod;
typedef void* Il2CppString;
typedef void* Il2CppThread;

static Il2CppDomain (*fp_domain_get)(void);
static const Il2CppAssembly** (*fp_domain_get_assemblies)(const Il2CppDomain*, size_t*);
static Il2CppImage* (*fp_assembly_get_image)(const Il2CppAssembly*);
static Il2CppClass* (*fp_class_from_name)(const Il2CppImage*, const char*, const char*);
static Il2CppMethod* (*fp_class_get_method_from_name)(Il2CppClass*, const char*, int);
static Il2CppString* (*fp_string_new)(const char*);
static Il2CppThread* (*fp_thread_attach)(const Il2CppDomain*);
static Il2CppThread* (*fp_thread_current)(void);
typedef void* Il2CppMethodInfo;
typedef void* Il2CppObject;
typedef void* Il2CppException;
typedef void* Il2CppField;
static Il2CppObject* (*fp_runtime_invoke)(Il2CppMethodInfo* method, void* obj, void** params, Il2CppException** exc);
static Il2CppObject* (*fp_object_new)(void* klass);
static Il2CppField* (*fp_class_get_field_from_name)(void* klass, const char* name);
static void (*fp_field_set_value_object)(void* obj, void* field, void* value);

static Il2CppDomain* g_domain = NULL;

static int load_api_elf(void) {
    int ok = 1;

    /* First try dlsym with the handle approach (original method) */
    void* handle = dlopen("libil2cpp.so", RTLD_NOW | RTLD_NOLOAD);
    if (handle) {
        write_log("dlopen handle obtained, trying dlsym...");
        #define L(sym, var) fp_##var = dlsym(handle, #sym); if(!fp_##var) ok=0;
        L(il2cpp_domain_get, domain_get);
        L(il2cpp_domain_get_assemblies, domain_get_assemblies);
        L(il2cpp_assembly_get_image, assembly_get_image);
        L(il2cpp_class_from_name, class_from_name);
        L(il2cpp_class_get_method_from_name, class_get_method_from_name);
        L(il2cpp_string_new, string_new);
        L(il2cpp_thread_attach, thread_attach);
        L(il2cpp_thread_current, thread_current);
        L(il2cpp_runtime_invoke, runtime_invoke);
        L(il2cpp_object_new, object_new);
        L(il2cpp_class_get_field_from_name, class_get_field_from_name);
        L(il2cpp_field_set_value_object, field_set_value_object);
        #undef L

        if (ok) {
            write_log("API loaded via dlsym (12 functions)");
            return 1;
        }
        write_log("dlsym failed for some symbols, falling back to ELF parse");
        /* Reset */
        ok = 1;
        fp_domain_get = NULL;
        fp_domain_get_assemblies = NULL;
        fp_assembly_get_image = NULL;
        fp_class_from_name = NULL;
        fp_class_get_method_from_name = NULL;
        fp_string_new = NULL;
        fp_thread_attach = NULL;
        fp_thread_current = NULL;
        fp_runtime_invoke = NULL;
        fp_object_new = NULL;
        fp_class_get_field_from_name = NULL;
        fp_field_set_value_object = NULL;
    } else {
        write_log("dlopen failed, using ELF parse only");
    }

    /* Fallback: ELF symbol table parse via dl_iterate_phdr */
    write_log("Trying dl_iterate_phdr + ELF parse...");
    g_lib_base = 0;
    dl_iterate_phdr(find_lib_callback, NULL);

    if (!g_lib_base) {
        write_log("ERROR: libil2cpp.so not found via dl_iterate_phdr");
        return 0;
    }

    char buf[256];
    snprintf(buf, sizeof(buf), "ELF parse: base=%p", (void*)g_lib_base);
    write_log(buf);

    /* Resolve each symbol */
    #define L(sym, var) do { \
        fp_##var = find_symbol_in_elf(g_lib_base, #sym); \
        if (!fp_##var) { write_log("ELF MISS: " #sym); ok=0; } \
        else { \
            snprintf(buf, sizeof(buf), "ELF OK: %s = %p", #sym, (void*)fp_##var); \
            write_log(buf); \
        } \
    } while(0)

    L(il2cpp_domain_get, domain_get);
    L(il2cpp_domain_get_assemblies, domain_get_assemblies);
    L(il2cpp_assembly_get_image, assembly_get_image);
    L(il2cpp_class_from_name, class_from_name);
    L(il2cpp_class_get_method_from_name, class_get_method_from_name);
    L(il2cpp_string_new, string_new);
    L(il2cpp_thread_attach, thread_attach);
    L(il2cpp_thread_current, thread_current);
    L(il2cpp_runtime_invoke, runtime_invoke);
    L(il2cpp_object_new, object_new);
    L(il2cpp_class_get_field_from_name, class_get_field_from_name);
    L(il2cpp_field_set_value_object, field_set_value_object);
    #undef L

    if (ok) {
        write_log("API loaded via ELF parse (12 functions)");
        return 1;
    }

    write_log("API load FAILED (both methods)");
    return 0;
}

/* ==== Crash guard ==== */
static sigjmp_buf g_jmp;

static void on_crash(int sig, siginfo_t* info, void* ctx) {
    siglongjmp(g_jmp, 1);
}

static struct sigaction g_old_segv, g_old_abrt;

static void guard_on(void) {
    struct sigaction sa;
    sa.sa_sigaction = on_crash;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, &g_old_segv);
    sigaction(SIGABRT, &sa, &g_old_abrt);
}

static void guard_off(void) {
    sigaction(SIGSEGV, &g_old_segv, NULL);
    sigaction(SIGABRT, &g_old_abrt, NULL);
}

#define SAFE(type, call) ({ \
    type _r = (type)0; \
    guard_on(); \
    if (sigsetjmp(g_jmp, 1) == 0) { \
        _r = call; \
    } \
    guard_off(); \
    _r; \
})

/* ==== Hook function ==== */
static Il2CppMethod* m_OnPurchaseSucceeded = 0;
static Il2CppMethod* m_OnPurchaseSuccessful = 0;
static Il2CppMethod* m_OnPurchaseFailed = 0;
static void* c_GooglePurchase = NULL;
static void* c_List = NULL;

static sigjmp_buf g_hook_jmp;
static volatile int g_hook_crashed = 0;
static volatile int g_hook_sig = 0;
static volatile void* g_hook_fault = NULL;

static void hook_crash_handler(int sig, siginfo_t* info, void* ctx) {
    g_hook_crashed = 1;
    g_hook_sig = sig;
    g_hook_fault = info->si_addr;
    siglongjmp(g_hook_jmp, 1);
}

static void copy_product_id(void* il2cpp_str, char* out, int max) {
    out[0] = 0;
    if (!il2cpp_str) return;
    int len = *(int*)((uintptr_t)il2cpp_str + 0x10);
    if (len <= 0 || len >= max) return;
    uint16_t* chars = (uint16_t*)((uintptr_t)il2cpp_str + 0x14);
    for (int i = 0; i < len; i++) out[i] = (char)chars[i];
    out[len] = 0;
}

static void* async_purchase_thread(void* arg) {
    if (fp_thread_attach && g_domain) {
        fp_thread_attach(g_domain);
        write_log("Async: thread attached to IL2CPP");
    }

    usleep(1000000);
    write_log("Async: attempting purchase completion...");

    extern void* g_async_gp_cb;
    extern void* g_async_mgr;
    extern char g_async_pid_str[256];

    if (!g_async_mgr || !g_async_pid_str[0]) {
        write_log("Async: mgr or pid empty");
        return NULL;
    }

    /* Dump PurchasingManager fields for debugging */
    {
        char buf[256];
        void* m_store = *(void**)((uintptr_t)g_async_mgr + 0x10);
        void* m_listener = *(void**)((uintptr_t)g_async_mgr + 0x18);
        void* products = *(void**)((uintptr_t)g_async_mgr + 0x70);
        snprintf(buf, sizeof(buf), "mgr dump: m_store=%p m_listener=%p products=%p", m_store, m_listener, products);
        write_log(buf);
    }

    if (!fp_string_new || !fp_object_new) {
        write_log("Async: string_new or object_new missing");
        return NULL;
    }

    void* product_id_str = fp_string_new(g_async_pid_str);
    void* receipt = fp_string_new("{}");
    void* tx_id = fp_string_new("fake_tx_001");
    void* purchase_token = fp_string_new("fake_token_001");

    char buf[512];

    struct sigaction sa, old_segv, old_abrt;
    sa.sa_sigaction = hook_crash_handler;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, &old_segv);
    sigaction(SIGABRT, &sa, &old_abrt);
    g_hook_crashed = 0;

    /* Method 1: Create fake GooglePurchase + call GooglePlayPurchaseCallback.OnPurchaseSuccessful */
    if (c_GooglePurchase && m_OnPurchaseSuccessful && g_async_gp_cb) {
        write_log("Async: trying OnPurchaseSuccessful with fake GooglePurchase...");
        Il2CppException* exc = NULL;

        void* fake_purchase = fp_object_new(c_GooglePurchase);
        snprintf(buf, sizeof(buf), "Async: fake_purchase=%p", fake_purchase);
        write_log(buf);

        if (fake_purchase) {
            /* Set GooglePurchase fields via direct offset writes (readonly bypass) */
            /* Offsets from dump.cs: 0x10=isAcknowledged(bool), 0x14=purchaseState(int),
               0x18=skus(List<string>), 0x20=orderId, 0x28=receipt, 0x30=signature,
               0x38=originalJson, 0x40=purchaseToken */
            *(uint8_t*)((uintptr_t)fake_purchase + 0x10) = 0;   /* isAcknowledged = false */
            *(int32_t*)((uintptr_t)fake_purchase + 0x14) = 0;   /* purchaseState = 0 (Purchased) */
            /* skus at offset 0x18 — skip, game lookup uses storeSpecificId string */
            *(uintptr_t*)((uintptr_t)fake_purchase + 0x20) = (uintptr_t)fp_string_new("fake_order_001");
            *(uintptr_t*)((uintptr_t)fake_purchase + 0x28) = (uintptr_t)receipt;
            *(uintptr_t*)((uintptr_t)fake_purchase + 0x30) = (uintptr_t)fp_string_new("fake_sig");
            *(uintptr_t*)((uintptr_t)fake_purchase + 0x38) = (uintptr_t)fp_string_new("{}");
            *(uintptr_t*)((uintptr_t)fake_purchase + 0x40) = (uintptr_t)purchase_token;
            write_log("Async: GooglePurchase fields set via offset write");
        }

        snprintf(buf, sizeof(buf), "Async: gp_cb=%p method=%p", g_async_gp_cb, *(void**)m_OnPurchaseSuccessful);
        write_log(buf);

        g_hook_crashed = 0;
        if (sigsetjmp(g_hook_jmp, 1) == 0) {
            if (fp_runtime_invoke) {
                void* params[3] = { fake_purchase, receipt, purchase_token };
                fp_runtime_invoke(m_OnPurchaseSuccessful, g_async_gp_cb, params, &exc);
            } else {
                typedef void (*fn_t)(void*, void*, void*, void*);
                fn_t fn = (fn_t)(*(void**)m_OnPurchaseSuccessful);
                fn(g_async_gp_cb, fake_purchase, receipt, purchase_token);
            }
            if (exc) {
                write_log("Async: OnPurchaseSuccessful returned exception");
            } else {
                write_log("Async: OnPurchaseSuccessful OK!");
            }
        } else {
            char cbuf[256];
            snprintf(cbuf, sizeof(cbuf), "Async: OnPurchaseSuccessful CRASHED sig=%d fault=%p", g_hook_sig, g_hook_fault);
            write_log(cbuf);
        }
    } else {
        snprintf(buf, sizeof(buf), "Async: skip OnPurchaseSuccessful (gp_cb=%p c_GP=%p m=%p)",
            g_async_gp_cb, c_GooglePurchase, m_OnPurchaseSuccessful);
        write_log(buf);
    }

    /* Method 2: Direct call to PurchasingManager.OnPurchaseSucceeded (last resort) */
    if (g_hook_crashed && m_OnPurchaseSucceeded) {
        write_log("Async: trying OnPurchaseSucceeded (last resort)...");
        g_hook_crashed = 0;
        if (sigsetjmp(g_hook_jmp, 1) == 0) {
            if (fp_runtime_invoke) {
                void* params[3] = { product_id_str, receipt, tx_id };
                Il2CppException* exc3 = NULL;
                fp_runtime_invoke(m_OnPurchaseSucceeded, g_async_mgr, params, &exc3);
            } else {
                typedef void (*fn_t)(void*, void*, void*, void*);
                fn_t fn = (fn_t)(*(void**)m_OnPurchaseSucceeded);
                fn(g_async_mgr, product_id_str, receipt, tx_id);
            }
            write_log("Async: OnPurchaseSucceeded OK!");
        } else {
            char cbuf[256];
            snprintf(cbuf, sizeof(cbuf), "Async: OnPurchaseSucceeded CRASHED sig=%d fault=%p", g_hook_sig, g_hook_fault);
            write_log(cbuf);
        }
    }

    sigaction(SIGSEGV, &old_segv, NULL);
    sigaction(SIGABRT, &old_abrt, NULL);
    return NULL;
}

void* g_async_gp_cb = NULL;
void* g_async_mgr = NULL;
char g_async_pid_str[256] = {0};

void hooked_purchase_entry(void* this_ptr, void* product_def, void* price_override) {
    write_log(">>> PURCHASE INTERCEPTED");

    if (!fp_string_new) {
        write_log("ERROR: not ready, blocking anyway");
        return;
    }

    void* product_id_ptr = product_def ? *(void**)((uintptr_t)product_def + 0x18) : NULL;
    g_async_pid_str[0] = 0;
    copy_product_id(product_id_ptr, g_async_pid_str, sizeof(g_async_pid_str));

    char buf[512];
    snprintf(buf, sizeof(buf), "Product: %s", g_async_pid_str);
    write_log(buf);

    void* gp_cb = *(void**)((uintptr_t)this_ptr + 0x30);
    void* mgr = gp_cb ? *(void**)((uintptr_t)gp_cb + 0x10) : NULL;
    if (!mgr) { write_log("ERROR: PurchasingManager NULL"); return; }

    snprintf(buf, sizeof(buf), "gp_cb=%p mgr=%p", gp_cb, mgr);
    write_log(buf);

    g_async_gp_cb = gp_cb;
    g_async_mgr = mgr;

    pthread_t tid;
    pthread_create(&tid, NULL, async_purchase_thread, NULL);
    pthread_detach(tid);

    write_log("Hook returning, async thread spawned");
}

/* ==== Init thread ==== */
static void* init_thread(void* arg) {
    write_log("=== SF2 IAP Bypass v49 ===");

    /* Wait for libil2cpp.so to be loaded by the game */
    int found = 0;
    for (int i = 0; i < 60; i++) {
        g_lib_base = 0;
        dl_iterate_phdr(find_lib_callback, NULL);
        if (g_lib_base) { found = 1; break; }
        usleep(500000);
    }
    if (!found) { write_log("ERROR: libil2cpp not found via dl_iterate_phdr"); return NULL; }

    /* Wait for IL2CPP runtime to initialize */
    write_log("Waiting 20s for IL2CPP runtime...");
    sleep(20);
    write_log("Wait done, loading API");

    if (!load_api_elf()) return NULL;

    /* Poll until all IL2CPP calls succeed */
    for (int attempt = 0; attempt < 120; attempt++) {
        char buf[256];

        Il2CppDomain* domain = SAFE(Il2CppDomain*, fp_domain_get());
        if (!domain) {
            if (attempt % 10 == 0) {
                snprintf(buf, sizeof(buf), "domain_get NULL, attempt %d", attempt);
                write_log(buf);
            }
            usleep(500000);
            continue;
        }
        g_domain = domain;

        size_t count = 0;
        const Il2CppAssembly** asms = SAFE(const Il2CppAssembly**, fp_domain_get_assemblies(domain, &count));
        if (!asms || count == 0) {
            if (attempt % 10 == 0) {
                snprintf(buf, sizeof(buf), "no assemblies, attempt %d", attempt);
                write_log(buf);
            }
            usleep(500000);
            continue;
        }

        snprintf(buf, sizeof(buf), "Got %zu assemblies", count);
        write_log(buf);

        Il2CppMethod* purchase_method = NULL;
        for (size_t i = 0; i < count; i++) {
            void* img = SAFE(void*, fp_assembly_get_image(asms[i]));
            if (!img) continue;
            void* klass = SAFE(void*, fp_class_from_name(img, "UnityEngine.Purchasing", "GooglePlayStore"));
            if (!klass) continue;
            snprintf(buf, sizeof(buf), "GooglePlayStore in asm %zu", i);
            write_log(buf);
            purchase_method = SAFE(Il2CppMethod*, fp_class_get_method_from_name(klass, "Purchase", 2));
            if (purchase_method) {
                snprintf(buf, sizeof(buf), "Purchase ptr=%p", *(void**)purchase_method);
                write_log(buf);
            }
            break;
        }

        for (size_t i = 0; i < count; i++) {
            void* img = SAFE(void*, fp_assembly_get_image(asms[i]));
            if (!img) continue;
            void* klass = SAFE(void*, fp_class_from_name(img, "UnityEngine.Purchasing", "PurchasingManager"));
            if (!klass) continue;
            m_OnPurchaseSucceeded = SAFE(Il2CppMethod*, fp_class_get_method_from_name(klass, "OnPurchaseSucceeded", 3));
            if (m_OnPurchaseSucceeded) {
                snprintf(buf, sizeof(buf), "OnPurchaseSucceeded ptr=%p", *(void**)m_OnPurchaseSucceeded);
                write_log(buf);
            }
            break;
        }

        /* Find GooglePurchase class */
        for (size_t i = 0; i < count; i++) {
            void* img = SAFE(void*, fp_assembly_get_image(asms[i]));
            if (!img) continue;
            void* klass = SAFE(void*, fp_class_from_name(img, "UnityEngine.Purchasing.Models", "GooglePurchase"));
            if (!klass) continue;
            c_GooglePurchase = klass;
            snprintf(buf, sizeof(buf), "GooglePurchase class=%p", klass);
            write_log(buf);
            break;
        }

        /* Find GooglePlayPurchaseCallback.OnPurchaseSuccessful */
        for (size_t i = 0; i < count; i++) {
            void* img = SAFE(void*, fp_assembly_get_image(asms[i]));
            if (!img) continue;
            void* klass = SAFE(void*, fp_class_from_name(img, "UnityEngine.Purchasing", "GooglePlayPurchaseCallback"));
            if (!klass) continue;
            m_OnPurchaseSuccessful = SAFE(Il2CppMethod*, fp_class_get_method_from_name(klass, "OnPurchaseSuccessful", 3));
            if (m_OnPurchaseSuccessful) {
                snprintf(buf, sizeof(buf), "OnPurchaseSuccessful ptr=%p", *(void**)m_OnPurchaseSuccessful);
                write_log(buf);
            }
            m_OnPurchaseFailed = SAFE(Il2CppMethod*, fp_class_get_method_from_name(klass, "OnPurchaseFailed", 2));
            if (m_OnPurchaseFailed) {
                snprintf(buf, sizeof(buf), "OnPurchaseFailed ptr=%p", *(void**)m_OnPurchaseFailed);
                write_log(buf);
            }
            break;
        }

        if (!purchase_method) {
            write_log("Methods not found yet");
            usleep(500000);
            continue;
        }

        long page = sysconf(_SC_PAGESIZE);
        void* orig_ptr = *(void**)purchase_method;

        void** slot = (void**)purchase_method;
        snprintf(buf, sizeof(buf), "MethodInfo rewrite: %p -> %p", orig_ptr, (void*)hooked_purchase_entry);
        write_log(buf);

        void* start = (void*)((uintptr_t)slot & ~(page - 1));
        if (mprotect(start, 2 * page, PROT_READ | PROT_WRITE) == 0) {
            slot[0] = (void*)hooked_purchase_entry;
            write_log("MethodInfo pointer rewritten");
        }

        void* klass_ptr = *(void**)((uintptr_t)purchase_method + 0x20);
        snprintf(buf, sizeof(buf), "Class=%p, scanning for vtable...", klass_ptr);
        write_log(buf);

        int vtable_found = 0;
        uintptr_t scan_start = (uintptr_t)klass_ptr;
        uintptr_t scan_end = scan_start + 0x2000;

        for (uintptr_t addr = scan_start; addr < scan_end; addr += sizeof(void*)) {
            void* val = *(void**)addr;
            if (val == orig_ptr) {
                snprintf(buf, sizeof(buf), "Vtable match at offset 0x%lx", (long)(addr - scan_start));
                write_log(buf);
                void* pg = (void*)(addr & ~(page - 1));
                if (mprotect(pg, page, PROT_READ | PROT_WRITE) == 0) {
                    *(void**)addr = (void*)hooked_purchase_entry;
                    vtable_found = 1;
                    write_log("Vtable entry rewritten!");
                }
                break;
            }
        }

        if (!vtable_found) {
            write_log("WARNING: vtable entry not found");
        }

        write_log("=== ALL REWRITES DONE ===");
        return NULL;
    }

    write_log("ERROR: init failed");
    return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    write_log("=== JNI_OnLoad v49 ===");
    pthread_t tid;
    pthread_create(&tid, NULL, init_thread, NULL);
    pthread_detach(tid);
    return JNI_VERSION_1_6;
}
