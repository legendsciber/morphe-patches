package app.extremecardriving.patches.currency

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.extremecardriving.patches.shared.Constants.COMPATIBILITY_ECD

// Şablon: Her yeni Unity oyununda bu dosyayı kopyala, sadece fingerprint ve SoBytes değiştir.
// Mantık: rawResourcePatch -> .so'yu APK lib'e ekle, bytecodePatch -> onCreate'te loadLibrary.
// Neden 2 patch? rawResourcePatch APK dosya sistemi için, bytecodePatch smali enjeksiyon için.
// Morphe'de tek patch'te ikisi bir arada olamaz - ayrı patch'ler gerekir, ikisi de default=true.
// Sonraki oyun: sadece COMPATIBILITY, Fingerprint, SoBytes ve loadLibrary ismi değişir.

// 1. Native lib'i APK'ye ekle (extractNativeLibs=false olduğu için lib/ Defl sıkıştırması kurulumu bozuyor,
// bu yüzden assets'e ekleyip runtime'da files dir'e kopyalıyoruz)
@Suppress("unused")
val ecdAddNativeLib = rawResourcePatch(
    name = "Extreme Car Driving Add Native Lib",
    description = "Adds libcurrencyhack.so to assets and loads via Runtime.load.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ECD)

    execute {
        val soFile = get("assets/libcurrencyhack.so", true)
        val bytes = SoBytes.part0() + SoBytes.part1() + SoBytes.part2() + SoBytes.part3() + SoBytes.part4() + SoBytes.part5() + SoBytes.part6() + SoBytes.part7() + SoBytes.part8() + SoBytes.part9() + SoBytes.part10() + SoBytes.part11() + SoBytes.part12()
        soFile.writeBytes(bytes)
    }
}

// 2. Smali enjeksiyon: ExtremeActivity.onCreate başına assets'ten kopyala ve Runtime.load
// Sonraki oyun: Fingerprint'i o oyunun MainActivity/UnityPlayerActivity ile değiştir.
@Suppress("unused")
val ecdCurrencyPatch = bytecodePatch(
    name = "Extreme Car Driving Unlimited Currencies",
    description = "Sets all in-game currencies (diamonds, coins, upgrade points) to 999,999,999 via IL2CPP API.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ECD)

    execute {
        // super.onCreate (filter 2) sonrası enjekte et - register pollution ve early init önlenir
        val idx = OnCreateFingerprint.instructionMatches[2].index + 1
        OnCreateFingerprint.method.addInstructions(idx, """
            const-string v0, "libcurrencyhack.so"
            invoke-virtual {p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;
            move-result-object v1
            invoke-virtual {v1, v0}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;
            move-result-object v1
            invoke-virtual {p0}, Landroid/content/Context;->getFilesDir()Ljava/io/File;
            move-result-object v0
            new-instance v2, Ljava/io/File;
            const-string v3, "libcurrencyhack.so"
            invoke-direct {v2, v0, v3}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
            new-instance v0, Ljava/io/FileOutputStream;
            invoke-direct {v0, v2}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
            invoke-static {v1, v0}, Landroidx/exifinterface/media/ExifInterfaceUtils;->copy(Ljava/io/InputStream;Ljava/io/OutputStream;)I
            move-result v3
            invoke-virtual {v0}, Ljava/io/FileOutputStream;->close()V
            invoke-virtual {v1}, Ljava/io/InputStream;->close()V
            invoke-virtual {v2}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;
            move-result-object v0
            invoke-static {v0}, Ljava/lang/System;->load(Ljava/lang/String;)V
        """.trimIndent())
    }
}
