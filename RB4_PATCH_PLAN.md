# Red Ball 4 Premium & Skin Unlock - Morphe Patch Plan

## Uygulama Bilgileri
- **Name**: Red Ball 4
- **Package**: `com.FDGEntertainment.redball4.gp`
- **Version**: 1.17.03 (code 11703)
- **Motor**: Unity IL2CPP (tüm oyun mantığı `libil2cpp.so` içinde)
- **Billing**: Google Play Billing v8.0.0 + Unity Purchasing v5.0.0
- **APK Type**: APK (anti-split ile birleştirilmiş)

## Hedef: Premium + Tüm Skin'leri Açma

### Kritik IL2CPP Method'lar ve Adresleri

| Method | Adres (libil2cpp.so) | Ne İşe Yarıyor | Patch |
|--------|----------------------|----------------|-------|
| `PremiumManager.get_IsPremiumUnlocked()` | `0x01121B70` | Premium durumunu döndüren getter | `MOV W0, #1; RET` → her zaman true |
| `GameVersion.get_HasNoAds()` | `0x0122F3A0` | Reklam yok mu kontrolü | `MOV W0, #1; RET` → her zaman true |
| `PlayerSkins.IsSkinUnlocked(int)` | `0x011347D8` | Skin kilidi kontrolü (8 skin var) | `MOV W0, #1; RET` → her zaman true |

### ARM64 Patch Bytes
```
52800020 = MOV W0, #1
D65F03C0 = RET
```
Toplam: 8 bytes per method, 24 bytes toplam.

### PremiumManager Sınıfı (dump.cs:149554)
```csharp
public class PremiumManager : MonoBehaviour {
    public static bool IsPremiumUnlocked { get; set; }  // RVA: 0x1121B70 / 0x11356EC
    public static bool IsPremiumGameVersion { get; }     // RVA: 0x11357B4
    public static void UnlockPremium() { }               // RVA: 0x11222B4
    public static void LockPremium() { }                 // RVA: 0x112448C
    private static void GivePremium() { }                // RVA: 0x1135740
    private static void OpenAllLevels() { }              // RVA: 0x1135C84
}
```

### GameVersion Sınıfı (dump.cs:152658)
```csharp
public class GameVersion : MonoBehaviour {
    public static bool HasNoAds { get; }        // RVA: 0x122F3A0
    public static void SetNoAds() { }           // RVA: 0x122F358
    public static bool IsPremiumGameVersion { get; }
}
```

### PlayerSkins Sınıfı (dump.cs:149471)
```csharp
public class PlayerSkins : MonoBehaviour {
    public const int NumberOfSkins = 8;
    public static bool IsSkinUnlocked(int skinIndex) { }  // RVA: 0x11347D8
    public static void UnlockSkin(int skinIndex, bool check = False) { } // RVA: 0x1134D0C
}
```

### UserData Kalıcı Veri (dump.cs:150260)
```csharp
private static Nullable<bool> premiumUnlocked; // offset 0x54
public static bool PremiumUnlocked { get; set; } // RVA: 0x1137670 / 0x11359E8
```

## Patch Stratejisi

### Neden Native .so Injection?
- Tüm oyun mantığı `libil2cpp.so` içinde (IL2CPP)
- Smali'de sadece Google Billing Client ve ad SDK wrapper'ları var
- PremiumManager, PlayerSkins gibi sınıflar MonoBehaviour → native kodda
- Smali seviyesinde patchleme mümkün değil

### Nasıl Çalışacak?
1. `legendsciber.so` APK'ye inject edilir (rawResourcePatch)
2. `UnityPlayerActivity.onCreate`'a `System.loadLibrary("legendsciber")` eklenir (bytecodePatch)
3. `JNI_OnLoad` tetiklenir:
   - `/proc/self/maps` ile `libil2cpp.so` base adresi bulunur
   - `mprotect` ile sayfalar writable yapılır
   - 3 method patch edilir (return true)
4. Oyun startsa premium açık, reklam yok, tüm skin'ler açık

## Repo Yapısı

```
patches/src/main/kotlin/app/redball4/patches/
├── shared/
│   └── Constants.kt              # COMPATIBILITY_REDBALL4
└── premium/
    ├── RedBall4PremiumPatch.kt   # rawResourcePatch + bytecodePatch
    ├── PremiumFingerprints.kt    # UnityPlayerActivity.onCreate fingerprint
    └── SoBytes.kt               # legendsciber.so byte array
```

### Constants.kt
- Package: `app.redball4.patches.shared`
- `COMPATIBILITY_REDBALL4`:
  - name = "Red Ball 4"
  - packageName = "com.FDGEntertainment.redball4.gp"
  - apkFileType = APK
  - appIconColor = 0xCC0000 (kırmızı top)
  - targets = [AppTarget(version = "1.17.03")]

### PremiumFingerprints.kt
- Package: `app.redball4.patches.premium`
- `OnCreateFingerprint`:
  - definingClass = `Lcom/unity3d/player/UnityPlayerActivity;`
  - name = "onCreate"
  - returnType = "V"
  - accessFlags = [PROTECTED]
  - parameters = ["Landroid/os/Bundle;"]
  - filters = [methodCall(definingClass="Lcom/unity3d/player/UnityPlayerActivity;", name="onCreate")]
  - Not: RB4'te custom Activity yok, direkt UnityPlayerActivity

### RedBall4PremiumPatch.kt
- Package: `app.redball4.patches.premium`
- Patch 1: `redBall4AddNativeLib = rawResourcePatch(...)`
  - `get("lib/arm64-v8a/legendsciber.so", true).writeBytes(SoBytes.bytes())`
- Patch 2: `redBall4PremiumUnlock = bytecodePatch(...)`
  - OnCreateFingerprint.instructionMatches[0].index + 1'e enjekte et:
  ```
  const-string p1, "legendsciber"
  invoke-static {p1}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
  ```

### SoBytes.kt
- Package: `app.redball4.patches.premium`
- `object SoBytes { fun bytes(): ByteArray = byteArrayOf(...) }`
- Compile edilmiş legendsciber.so binary'si (imzalanmamış, Android NDK Bionic)

## legendsciber.c Kaynak Kodu

```c
#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>

#define LIBIL2CPP_BASE_OFFSET 0x01121B70  // PremiumManager.get_IsPremiumUnlocked
#define HASNOADS_OFFSET       0x0122F3A0  // GameVersion.get_HasNoAds
#define SKINUNLOCKED_OFFSET   0x011347D8  // PlayerSkins.IsSkinUnlocked

static unsigned long get_libil2cpp_base() {
    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) return 0;
    char line[512];
    unsigned long base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, "libil2cpp.so") && strstr(line, "r-xp")) {
            base = strtoul(line, NULL, 16);
            break;
        }
    }
    fclose(fp);
    return base;
}

static void patch_method(void *addr) {
    long page_size = sysconf(_SC_PAGESIZE);
    void *page_start = (void *)((unsigned long)addr & ~(page_size - 1));
    mprotect(page_start, page_size, PROT_READ | PROT_WRITE | PROT_EXEC);
    unsigned int *inst = (unsigned int *)addr;
    inst[0] = 0x52800020;  // MOV W0, #1
    inst[1] = 0xD65F03C0;  // RET
}

__attribute__((constructor))
void on_load() {
    unsigned long base = get_libil2cpp_base();
    if (!base) return;
    patch_method((void *)(base + LIBIL2CPP_BASE_OFFSET));
    patch_method((void *)(base + HASNOADS_OFFSET));
    patch_method((void *)(base + SKINUNLOCKED_OFFSET));
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_6;
}
```

## Compile
```bash
aarch64-linux-android-clang -shared -o legendsciber.so legendsciber.c -landroid
```

## Adımlar
1. SF2 klasörünü repodan sil (localde kalır)
2. RB4 dizin yapısını oluştur
3. Constants.kt yaz
4. PremiumFingerprints.kt yaz
5. legendsciber.c yaz ve compile et
6. SoBytes.kt oluştur (byte array)
7. RedBall4PremiumPatch.kt yaz
8. Gradle build test (opsiyonel)
