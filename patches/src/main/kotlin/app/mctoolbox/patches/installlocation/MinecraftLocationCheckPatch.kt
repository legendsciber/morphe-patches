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
 *   MinecraftActivity sitesleri (komut sirasi):
 *     [0]=:533 genel kapı  -> ZORLANIR
 *     [1]=:655 TERS semantik (true -> hata) -> ATLANIR
 *     [2]=:801 ABI kapısı   -> ZORLANIR
 *     [3]=:875 ABI kapısı   -> ZORLANIR
 *   RelaunchActivity: tum c() siteleri zorlanir.
 *
 * Enjeksiyon noktasi her sitede invoke(+0)/move-result(+1)/dal(+2) seklinde;
 * v5 result register'i move-result ile yazilip dal tarafindan okundugu icin
 * araya giren const/4 v5,0x1 verifier acisindan guvenlidir (int->int).
 */
@Suppress("unused")
val mctoolboxVersionUnlockPatch = bytecodePatch(
    name = "Unlock All MCPE Versions",
    description = "Desteklenen surum kontrolu her zaman gecer: liste disi Minecraft surumlerinde \"not supported\" ve 32/64-bit uyari ekrani cikmaz.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // MinecraftActivity: yalnizca [0], [2], [3] — ters semantikli [1] atlanir
        val ma = McSupportedVersionFingerprint
        val maIdx = ma.instructionMatches.map { it.index }
        listOfNotNull(
            maIdx.getOrNull(0),
            maIdx.getOrNull(2),
            maIdx.getOrNull(3)
        ).sortedDescending().forEach { idx ->
            ma.method.addInstructions(idx + 2, """
                const/4 v5, 0x1
            """.trimIndent())
        }

        // RelaunchActivity: tum c() siteleri
        val ra = RelaunchSupportedVersionFingerprint
        ra.instructionMatches
            .map { it.index }
            .sortedDescending()
            .forEach { idx ->
                ra.method.addInstructions(idx + 2, """
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
