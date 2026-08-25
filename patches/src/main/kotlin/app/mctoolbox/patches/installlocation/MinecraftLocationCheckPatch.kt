package app.mctoolbox.patches.installlocation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * 1) Unlock All MCPE Versions — surum kontrolu hep "destekleniyor" doner.
 *
 * Gercek MCPE PackageInfo'su sorgulanmaya devam eder (paket adi DEGISTIRILMEZ),
 * yalnizca surum karsilastirmalarinin sonucu zorlanir:
 *
 *   - MinecraftActivity ilk kapı (:533)
 *   - RelaunchActivity ilk kapısı
 *   - ABI zincirli iki kapı (:801/:875) — Toolbox hangi bitlikte calisirsa
 *     calissin "not_supported_64bit / _32bit" uyarisi cikmaz
 *
 * Bilinçli istisna: :655 sitesinin semantigi TERSDIR (true -> hata); 32-bit
 * MCPE ile a()=false akisinda o bloga girilmedigi icin dokunulmasina gerek
 * yoktur.
 */
@Suppress("unused")
val mctoolboxVersionUnlockPatch = bytecodePatch(
    name = "Unlock All MCPE Versions",
    description = "Desteklenen surum kontrolu her zaman gecer: liste disi Minecraft surumlerinde \"not supported\" ve 32/64-bit uyari ekrani cikmaz.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Ilk genel kapilar
        McSupportedVersionFingerprint.method.addInstructions(
            McSupportedVersionFingerprint.instructionMatches[0].index + 2,
            """
                const/4 v5, 0x1
            """.trimIndent()
        )

        RelaunchSupportedVersionFingerprint.method.addInstructions(
            RelaunchSupportedVersionFingerprint.instructionMatches[0].index + 2,
            """
                const/4 v5, 0x1
            """.trimIndent()
        )

        // ABI zincirli kapilar (:801/:875) — azalan sira, indeks kaymasin
        val gateFp = McAbiVersionGateFingerprint
        gateFp.instructionMatches
            .map { it.index }
            .sortedDescending()
            .forEach { idx ->
                gateFp.method.addInstructions(idx + 2, """
                    const/4 v5, 0x1
                """.trimIndent())
            }
    }
}

/**
 * 2) Spoof Google Play Installer — kurulum kaynagi kontrolunu gecersiz kil.
 *
 * getInstallerPackageName(MCPE) zinciri, MCPE Google Play'den kurulmus gibi
 * davranmaya zorlanir ("com.android.vending" deseni): prefs'e test="0"
 * yazilir; kurulum kaynagina bagli lisans/deneme yollari acilir.
 */
@Suppress("unused")
val mctoolboxPlaySpoofPatch = bytecodePatch(
    name = "Bypass Google Play Install Check",
    description = "Minecraft'in nereden yuklendigi kontrol edilmez: her zaman Google Play'den kurulmus gibi islem gorur.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        McInstallerCheckFingerprint.method.addInstructions(
            McInstallerCheckFingerprint.instructionMatches[0].index + 2,
            """
                const/4 v5, 0x1
            """.trimIndent()
        )
    }
}
