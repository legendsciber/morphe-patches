#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <dlfcn.h>
#include <sys/mman.h>

#define LOG_TAG "ShadowHack"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#include <android/log.h>

/*
 * Shadow Fight 2 - Hash Bypass v24
 *
 * Target: KLPJOKOFLJD.LFENGGKOJDO(string, string) -> bool
 * RVA: 0x34B7AD0
 * Purpose: This method verifies .hash files against .xml/.bin files.
 *          We hook it to always return true, so any modified XML is accepted.
 *
 * ARM64 hook:
 *   mov w0, #1   (0x52800020)
 *   ret           (0xD65F03C0)
 */

#define LFENGGKOJDO_RVA 0x34B7AD0

static uintptr_t find_libil2cpp_base() {
    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) return 0;

    char line[512];
    uintptr_t base = 0;

    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, "libil2cpp.so") && strstr(line, "r-xp")) {
            uintptr_t start;
            sscanf(line, "%lx-", &start);
            base = start;
            break;
        }
    }
    fclose(fp);
    return base;
}

static int hook_method(uintptr_t addr) {
    long page = sysconf(_SC_PAGESIZE);
    void *page_start = (void *)(addr & ~(page - 1));

    if (mprotect(page_start, page, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        LOGE("mprotect failed for %p", page_start);
        return 0;
    }

    uint32_t instructions[2] = {
        0x52800020,  /* mov w0, #1 */
        0xD65F03C0   /* ret */
    };

    memcpy((void *)addr, instructions, 8);

    /* ARM64 icache flush via DC CVAU + IC IVAU + DSB + ISB */
    uintptr_t line = addr & ~15UL;
    for (uintptr_t i = line; i < addr + 8; i += 16) {
        __asm__ volatile("dc cvau, %0" :: "r"(i));
        __asm__ volatile("ic ivau, %0" :: "r"(i));
    }
    __asm__ volatile("dsb ish");
    __asm__ volatile("isb");

    return 1;
}

static void write_log(const char *msg) {
    FILE *fp = fopen("/sdcard/Download/shadowhardcode-log.txt", "a");
    if (fp) {
        fprintf(fp, "%s\n", msg);
        fclose(fp);
    }
}

__attribute__((visibility("default")))
JNIEXPORT void JNICALL
Java_app_shadowfight_patches_hardcode_ExploitService_nativeInit(JNIEnv *env, jobject thiz) {
    LOGI("=== ShadowFight Hash Bypass v24 ===");
    write_log("=== v24: Hash Bypass (KLPJOKOFLJD.LFENGGKOJDO hook) ===");

    uintptr_t il2cpp_base = find_libil2cpp_base();
    if (!il2cpp_base) {
        LOGE("Could not find libil2cpp.so base address!");
        write_log("ERROR: libil2cpp.so not found");
        return;
    }
    LOGI("libil2cpp.so base: 0x%lx", (long)il2cpp_base);

    uintptr_t target = il2cpp_base + LFENGGKOJDO_RVA;
    LOGI("Target method addr: 0x%lx (base + 0x%x)", (long)target, LFENGGKOJDO_RVA);

    if (hook_method(target)) {
        LOGI("Hook installed successfully!");
        write_log("SUCCESS: Hash verification bypassed");

        uint32_t *code = (uint32_t *)target;
        LOGI("Verify: [0]=0x%08x [1]=0x%08x", code[0], code[1]);
    } else {
        LOGE("Hook installation FAILED!");
        write_log("ERROR: Hook installation failed");
    }

    LOGI("=== Done. Edit users.xml and restart game. ===");
    write_log("=== Users can now edit users.xml freely ===");
}
