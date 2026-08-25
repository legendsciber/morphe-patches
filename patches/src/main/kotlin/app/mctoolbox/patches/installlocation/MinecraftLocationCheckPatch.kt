package app.mctoolbox.patches.installlocation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

private const val TOOLBOX_PACKAGE = "io.mrarm.mctoolbox"

/**
 * 1) Bypass Install Location Check
 *
 * MinecraftActivity.onCreate ve RelaunchActivity.onCreate icindeki tek
 * "com.mojang.minecraftpe" sabitini Toolbox paket adiyla degistirir.
 * PackageManager kendi paketini her zaman cozer; boylece NameNotFoundException
 * (:catch_2) yolu olu, "not_installed / Game not found" hatasi asla cikmaz ve
 * V:PackageInfo alani tutarli kalir.
 */
@Suppress("unused")
val mctoolboxInstallCheckPatch = bytecodePatch(
    name = "Bypass Install Location Check",
    description = "Minecraft kurulum konumu kontrolu hep basarili olur: paket sorgusu uygulamanin kendisine yonlendirilir (\"not installed\" hatasi asla cikmaz).",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        McPackageLookupFingerprint.method.apply {
            val idx = McPackageLookupFingerprint.instructionMatches[0].index
            removeInstructions(idx, 1)
            addInstructions(idx, """
                const-string v0, "$TOOLBOX_PACKAGE"
            """.trimIndent())
        }

        RelaunchPackageLookupFingerprint.method.apply {
            val idx = RelaunchPackageLookupFingerprint.instructionMatches[0].index
            removeInstructions(idx, 1)
            addInstructions(idx, """
                const-string v3, "$TOOLBOX_PACKAGE"
            """.trimIndent())
        }
    }
}

/**
 * 2) Unlock All MCPE Versions
 *
 * Genel surum kapilarinin sonucunu 1'e zorlar:
 *   - MinecraftActivity.onCreate ilk Li60.c() (:533)  -> "not_supported" engellenir
 *   - RelaunchActivity.onCreate ilk Li60.c()          -> relaunch yolunda ayni
 * Boylece destek listesinde olmayan MCPE surumleri de kabul edilir.
 */
@Suppress("unused")
val mctoolboxVersionUnlockPatch = bytecodePatch(
    name = "Unlock All MCPE Versions",
    description = "Desteklenen surum kontrolu her zaman gecer: liste disi Minecraft surumlerinde \"not supported\" hatasi cikmaz.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // SADECE ilk surum kapilari zorlanir (:533 ve RA'daki karsiligi).
        // Diger c() sitelerinden :655'in semantigi TERSDIR (true -> hata),
        // :801/:875 ise AbiGatePatch'in isidir — buraya dokunulmaz.
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
    }
}

/**
 * 3) Bypass 64-bit Architecture Gate  (VARSAYILAN: KAPALI)
 *
 * La0.b(ApplicationInfo) iki noktada MCPE'nin 64-bit olup olmadigini sorar;
 * true donerse ya "64bit_on_32bit" uyarisi cikar ya da Toolbox 64-bit olarak
 * yeniden baslatilir (RelaunchActivity). Uyusmayan bir MCPE ile bu yol crash
 * verebilir.
 *
 * Bu yama her iki kontrolun sonucunu 0 yapar: Toolbox calistigi bitlikte
 * devam eder, relaunch/hata yollari tamamen devre disi kalir.
 *
 * UYARI: Gercek bir mimari uyusmazligi varsa (32-bit Toolbox + 64-bit MCPE)
 * native kutuphane yuklemesi crash verebilir. O durumda bu yamayi kapali
 * tutup mimarisine uygun MCPE surumunu kurun.
 */
@Suppress("unused")
val mctoolboxAbiGatePatch = bytecodePatch(
    name = "Bypass 64-bit Architecture Gate",
    description = "64-bit mimari kontrolunu devre disi birakir: \"Unsupported 64-bit\" uyarisi ve otomatik 64-bit yeniden baslatma olmaz. Uyumsuz mimaride crash riski var — varsayilan kapali.",
    default = false
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Katman 1: La0.b() iki cagri noktasini da 0'a zorla — MCPE "mimari
        // uyumsuz" sayilmaz; 64-bit relaunch ve _on_32bit uyarisi hic
        // tetiklenmez. (Azalan sira: ekleme indeksleri kaymasin.)
        val bFp = McAbiRelaunchFingerprint
        bFp.instructionMatches
            .map { it.index }
            .sortedDescending()
            .forEach { idx ->
                bFp.method.addInstructions(idx + 2, """
                    const/4 v5, 0x0
                """.trimIndent())
            }

        // Katman 2: a()-zincirli iki surum kapisini (:801/:875) 1'e zorla —
        // Toolbox hangi bitlikte calisirsa calissin surum reddi olmaz.
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
 * 4) Spoof Google Play Installer  (VARSAYILAN: KAPALI)
 *
 * getInstallerPackageName(MCPE) zinciri, MCPE Google Play'den kurulmus gibi
 * davranmaya zorlanir ("com.android.vending" deseni): prefs'e test="0"
 * yazilir, Play-kurulumuna bagli lisans/deneme yollari acilir.
 *
 * Yan etkileri oldugu icin varsayilan kapali gelir; ihtiyac halinde Morphe
 * icinden acin.
 */
@Suppress("unused")
val mctoolboxPlaySpoofPatch = bytecodePatch(
    name = "Spoof Google Play Installer",
    description = "Minecraft Play Store'dan kurulmus gibi gosterilir; kurulum kaynagina bagli kontroller gecer.",
    default = false
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
