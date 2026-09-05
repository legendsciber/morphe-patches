#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#include <pthread.h>

#define LOG_TAG "ShadowHack"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#include <android/log.h>

/*
 * Shadow Fight 2 - Hash Bypass v24
 * Target: KLPJOKOFLJD.LFENGGKOJDO(string, string) -> bool @ RVA 0x34B7AD0
 * Polls for libil2cpp.so, then hooks.
 */

#define LFENGGKOJDO_RVA 0x34B7AD0

static void write_log(const char *msg) {
    FILE *fp = fopen("/sdcard/Download/shadowhardcode-log.txt", "a");
    if (fp) {
        fprintf(fp, "%s\n", msg);
        fclose(fp);
    }
}

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

    if (mprotect(page_start, page * 2, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        LOGE("mprotect failed for %p", page_start);
        return 0;
    }

    uint32_t instructions[2] = {
        0x52800020,
        0xD65F03C0
    };

    memcpy((void *)addr, instructions, 8);

    uintptr_t line_addr = addr & ~15UL;
    for (uintptr_t i = line_addr; i < addr + 8; i += 16) {
        __asm__ volatile("dc cvau, %0" :: "r"(i));
        __asm__ volatile("ic ivau, %0" :: "r"(i));
    }
    __asm__ volatile("dsb ish");
    __asm__ volatile("isb");

    return 1;
}

static void *hook_thread(void *arg) {
    write_log("v24: poll thread started, waiting for libil2cpp.so...");

    for (int i = 0; i < 200; i++) {
        uintptr_t il2cpp_base = find_libil2cpp_base();
        if (il2cpp_base) {
            LOGI("libil2cpp.so found at: 0x%lx (attempt %d)", (long)il2cpp_base, i);
            write_log("libil2cpp.so found");

            uintptr_t target = il2cpp_base + LFENGGKOJDO_RVA;
            LOGI("Target: 0x%lx (base + 0x%x)", (long)target, LFENGGKOJDO_RVA);

            if (hook_method(target)) {
                LOGI("Hook OK!");
                write_log("SUCCESS: LFENGGKOJDO hooked -> always return true");

                uint32_t *code = (uint32_t *)target;
                LOGI("Verify: [0]=0x%08x [1]=0x%08x", code[0], code[1]);
            } else {
                LOGE("Hook FAILED!");
                write_log("ERROR: Hook failed");
            }
            return NULL;
        }
        usleep(100000); /* 100ms between checks */
    }

    write_log("ERROR: libil2cpp.so not found after 20s");
    return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    write_log("=== v24: JNI_OnLoad triggered ===");

    pthread_t tid;
    pthread_create(&tid, NULL, hook_thread, NULL);
    pthread_detach(tid);

    return JNI_VERSION_1_6;
}
