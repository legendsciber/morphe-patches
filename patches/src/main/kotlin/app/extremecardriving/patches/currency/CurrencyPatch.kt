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

// 1. Native lib'i APK'ye ekle (APKM lib/arm64-v8a/ için)
@Suppress("unused")
val ecdAddNativeLib = rawResourcePatch(
    name = "Extreme Car Driving Add Native Lib",
    description = "Adds libcurrencyhack.so to the APK lib directory.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ECD)

    execute {
        val soFile = get("lib/arm64-v8a/libcurrencyhack.so", true)
        val bytes = SoBytes.part0() + SoBytes.part1() + SoBytes.part2() + SoBytes.part3() + SoBytes.part4() + SoBytes.part5() + SoBytes.part6() + SoBytes.part7() + SoBytes.part8() + SoBytes.part9() + SoBytes.part10() + SoBytes.part11() + SoBytes.part12()
        soFile.writeBytes(bytes)
    }
}

// 2. Smali enjeksiyon: ExtremeActivity.onCreate başına System.loadLibrary
// Sonraki oyun: Fingerprint'i o oyunun MainActivity/UnityPlayerActivity ile değiştir.
@Suppress("unused")
val ecdCurrencyPatch = bytecodePatch(
    name = "Extreme Car Driving Unlimited Currencies",
    description = "Sets all in-game currencies (diamonds, coins, upgrade points) to 999,999,999 via IL2CPP API.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ECD)

    execute {
        OnCreateFingerprint.method.addInstructions(0, """
            const-string v0, "currencyhack"
            invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
        """.trimIndent())
    }
}
