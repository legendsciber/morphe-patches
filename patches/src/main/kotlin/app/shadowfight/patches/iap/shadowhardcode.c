#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#include <pthread.h>
#include <stdint.h>
#include <dlfcn.h>

#define LOG_TAG "SF2IAP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#include <android/log.h>

/*
 * Shadow Fight 2 - IAP Bypass v25
 *
 * Hook 1: IAGKBFCKFKB (RVA 0x340CE28)
 *   KGNNFOHIBBD == 3 (RealMoney) -> changes to 1 (Gold)
 *   Google Play never launches, purchase completes as Gold
 *
 * Hook 2: HDDFDBIKKFH (RVA 0x305A128)
 *   Always sets currency amount to 999999
 *
 * Result: Click "Buy for $X" -> uses Gold instead -> Gold is always 999999 -> success
 */

#define IAGKBFCKFKB_RVA 0x340CE28
#define HDDFDBIKKFH_RVA 0x305A128
#define ENTRY_SIZE 16

static void write_log(const char* msg) {
    FILE* fp = fopen("/sdcard/Download/sf2-iap-log.txt", "a");
    if (fp) { fprintf(fp, "%s\n", msg); fclose(fp); }
}

static uintptr_t find_libil2cpp() {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return 0;
    char line[512];
    uintptr_t base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, "libil2cpp.so")) {
            sscanf(line, "%lx-", &base);
            break;
        }
    }
    fclose(fp);
    return base;
}

static int make_writable(void* addr, size_t len) {
    long page = sysconf(_SC_PAGESIZE);
    void* start = (void*)((uintptr_t)addr & ~(page - 1));
    int ret = mprotect(start, len + page, PROT_READ | PROT_WRITE | PROT_EXEC);
    if (ret != 0) {
        LOGE("mprotect failed for %p: %d", addr, ret);
        write_log("ERROR: mprotect failed");
    }
    return ret == 0;
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

/*
 * IAPK stub (24 bytes):
 *   cmp w1, #3           ; RealMoney?
 *   b.ne +4              ; skip if not
 *   mov w1, #1           ; change to Gold
 *   ldr x16, [pc, #8]   ; load trampoline
 *   br x16
 *   .quad trampoline
 */
static uint8_t IAPK_STUB[28] __attribute__((aligned(16)));

/* HDDF stub (24 bytes):
 *   movz w1, #0x423F     ; w1 = 999999 low
 *   movk w1, #0x000F, lsl #16 ; w1 = 999999
 *   ldr x16, [pc, #8]   ; load trampoline
 *   br x16
 *   .quad trampoline
 */
static uint8_t HDDF_STUB[24] __attribute__((aligned(16)));

static uint8_t TRAMP_IAPK[32] __attribute__((aligned(16)));
static uint8_t TRAMP_HDDF[32] __attribute__((aligned(16)));

static void* hook_thread(void* arg) {
    write_log("=== SF2 IAP Bypass v26 ===");
    LOGI("Polling for libil2cpp.so...");

    uintptr_t il2cpp_base = 0;
    for (int i = 0; i < 200; i++) {
        il2cpp_base = find_libil2cpp();
        if (il2cpp_base) break;
        usleep(100000);
    }

    if (!il2cpp_base) {
        write_log("ERROR: libil2cpp.so not found");
        return NULL;
    }
    LOGI("libil2cpp.so base: 0x%lx (first mapping)", (long)il2cpp_base);
    { char buf[128]; snprintf(buf, sizeof(buf), "libil2cpp.so base=0x%lx", (long)il2cpp_base); write_log(buf); }

    /* Build IAPK_STUB */
    memset(IAPK_STUB, 0, sizeof(IAPK_STUB));
    /* cmp w1, #3 */
    IAPK_STUB[0] = 0x3F; IAPK_STUB[1] = 0x0C; IAPK_STUB[2] = 0x00; IAPK_STUB[3] = 0x71;
    /* b.ne +2 (skip mov, jump to ldr x16 at offset 12) */
    IAPK_STUB[4] = 0x41; IAPK_STUB[5] = 0x00; IAPK_STUB[6] = 0x00; IAPK_STUB[7] = 0x54;
    /* mov w1, #1 */
    IAPK_STUB[8] = 0x21; IAPK_STUB[9] = 0x00; IAPK_STUB[10] = 0x80; IAPK_STUB[11] = 0x52;
    /* ldr x16, [pc, #8] -> loads from offset 16 */
    IAPK_STUB[12] = 0x50; IAPK_STUB[13] = 0x00; IAPK_STUB[14] = 0x00; IAPK_STUB[15] = 0x58;
    /* br x16 */
    IAPK_STUB[16] = 0x00; IAPK_STUB[17] = 0x02; IAPK_STUB[18] = 0x1F; IAPK_STUB[19] = 0xD6;
    /* literal pool: trampoline address (offset 20) - patched below */

    /* Build HDDF_STUB */
    memset(HDDF_STUB, 0, sizeof(HDDF_STUB));
    /* movz w1, #0x423F */
    HDDF_STUB[0] = 0xE1; HDDF_STUB[1] = 0x84; HDDF_STUB[2] = 0x92; HDDF_STUB[3] = 0x52;
    /* movk w1, #0x000F, lsl #16 */
    HDDF_STUB[4] = 0xE1; HDDF_STUB[5] = 0x01; HDDF_STUB[6] = 0xA0; HDDF_STUB[7] = 0x72;
    /* ldr x16, [pc, #8] -> loads from offset 16 */
    HDDF_STUB[8] = 0x50; HDDF_STUB[9] = 0x00; HDDF_STUB[10] = 0x00; HDDF_STUB[11] = 0x58;
    /* br x16 */
    HDDF_STUB[12] = 0x00; HDDF_STUB[13] = 0x02; HDDF_STUB[14] = 0x1F; HDDF_STUB[15] = 0xD6;
    /* literal pool: trampoline address (offset 16) - patched below */

    /* === Hook 1: IAGKBFCKFKB === */
    uintptr_t iapk_target = il2cpp_base + IAGKBFCKFKB_RVA;
    LOGI("IAGKBFCKFKB @ 0x%lx", (long)iapk_target);

    /* Build trampoline */
    memcpy(TRAMP_IAPK, (void*)iapk_target, ENTRY_SIZE);
    { char buf[128]; snprintf(buf, sizeof(buf), "Trampoline built from 0x%lx", (long)iapk_target); write_log(buf); }
    TRAMP_IAPK[16] = 0x50; TRAMP_IAPK[17] = 0x00; TRAMP_IAPK[18] = 0x00; TRAMP_IAPK[19] = 0x58;
    TRAMP_IAPK[20] = 0x00; TRAMP_IAPK[21] = 0x02; TRAMP_IAPK[22] = 0x1F; TRAMP_IAPK[23] = 0xD6;
    uintptr_t tramp_iapk_cont = iapk_target + ENTRY_SIZE;
    memcpy(TRAMP_IAPK + 24, &tramp_iapk_cont, 8);

    /* Patch stub trampoline address */
    uintptr_t tramp_iapk_ptr = (uintptr_t)TRAMP_IAPK;
    memcpy(IAPK_STUB + 20, &tramp_iapk_ptr, 8);

    /* Write entry hook */
    if (!make_writable((void*)iapk_target, 32)) {
        write_log("ERROR: mprotect failed for IAGKBFCKFKB");
        return NULL;
    }
    uint8_t entry[ENTRY_SIZE];
    entry[0] = 0x50; entry[1] = 0x00; entry[2] = 0x00; entry[3] = 0x58;
    entry[4] = 0x00; entry[5] = 0x02; entry[6] = 0x1F; entry[7] = 0xD6;
    uintptr_t stub_ptr = (uintptr_t)IAPK_STUB;
    memcpy(entry + 8, &stub_ptr, 8);
    memcpy((void*)iapk_target, entry, ENTRY_SIZE);
    flush_icache((void*)iapk_target, ENTRY_SIZE);
    write_log("IAGKBFCKFKB hook installed (RealMoney->Gold)");

    /* === Hook 2: HDDFDBIKKFH === */
    uintptr_t hddf_target = il2cpp_base + HDDFDBIKKFH_RVA;
    LOGI("HDDFDBIKKFH @ 0x%lx", (long)hddf_target);

    memcpy(TRAMP_HDDF, (void*)hddf_target, ENTRY_SIZE);
    { char buf[128]; snprintf(buf, sizeof(buf), "Trampoline built from 0x%lx", (long)hddf_target); write_log(buf); }
    TRAMP_HDDF[16] = 0x50; TRAMP_HDDF[17] = 0x00; TRAMP_HDDF[18] = 0x00; TRAMP_HDDF[19] = 0x58;
    TRAMP_HDDF[20] = 0x00; TRAMP_HDDF[21] = 0x02; TRAMP_HDDF[22] = 0x1F; TRAMP_HDDF[23] = 0xD6;
    uintptr_t tramp_hddf_cont = hddf_target + ENTRY_SIZE;
    memcpy(TRAMP_HDDF + 24, &tramp_hddf_cont, 8);

    uintptr_t tramp_hddf_ptr = (uintptr_t)TRAMP_HDDF;
    memcpy(HDDF_STUB + 16, &tramp_hddf_ptr, 8);

    if (!make_writable((void*)hddf_target, 32)) {
        write_log("ERROR: mprotect failed for HDDFDBIKKFH");
        return NULL;
    }
    entry[0] = 0x50; entry[1] = 0x00; entry[2] = 0x00; entry[3] = 0x58;
    entry[4] = 0x00; entry[5] = 0x02; entry[6] = 0x1F; entry[7] = 0xD6;
    stub_ptr = (uintptr_t)HDDF_STUB;
    memcpy(entry + 8, &stub_ptr, 8);
    memcpy((void*)hddf_target, entry, ENTRY_SIZE);
    flush_icache((void*)hddf_target, ENTRY_SIZE);
    write_log("HDDFDBIKKFH hook installed (amount=999999)");

    /* Verify */
    uint32_t* v1 = (uint32_t*)iapk_target;
    uint32_t* v2 = (uint32_t*)hddf_target;
    LOGI("IAGKBFCKFKB: [0]=0x%08x [1]=0x%08x", v1[0], v1[1]);
    LOGI("HDDFDBIKKFH: [0]=0x%08x [1]=0x%08x", v2[0], v2[1]);
    write_log("=== All hooks installed ===");
    return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    write_log("=== JNI_OnLoad ===");
    pthread_t tid;
    pthread_create(&tid, NULL, hook_thread, NULL);
    pthread_detach(tid);
    return JNI_VERSION_1_6;
}
