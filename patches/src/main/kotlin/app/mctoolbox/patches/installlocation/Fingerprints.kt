package app.mctoolbox.patches.installlocation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * MinecraftActivity.onCreate — metodun İLK talimatı
 * (const-string "com.mojang.minecraftpe", index 0).
 *
 * Paket-redirect yamasi kaldirildi; bu fingerprint artik PlaySpoof yamasinin
 * METOT BASINA pref-yazimi enjekte etmesi icin sabit cipa olarak kullanilir
 * (index 0 garanti, kayma riski yok).
 */
object McPackageLookupFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/MinecraftActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        string("com.mojang.minecraftpe")
    )
)

/**
 * MinecraftActivity.onCreate (:533) — genel surum kapisi.
 *
 * Tek filtreli oldugu icin instructionMatches, metoddaki TUM Li60.c
 * cagrilariyla KOMUT SIRASINA gore eslesir:
 *
 *   matches[0] = :533  genel surum kapisi        (0 -> "not_supported")
 *   matches[1] = :655  TERS SEMANTIK!            (TRUE -> "64bit_on_32bit")
 *   matches[2] = :801  ABI surum kapisi          (0 -> "not_supported_64bit")
 *   matches[3] = :875  ABI surum kapisi          (0 -> "not_supported_32bit")
 *
 * VersionUnlockPatch yalnizca [0], [2] ve [3]'u zorlar; [1]'e DOKUNULMAZ.
 */
object McSupportedVersionFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/MinecraftActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(definingClass = "Li60;", name = "c")
    )
)

/**
 * RelaunchActivity.onCreate — genel surum kapisi (ilk Li60.c cagrisi).
 */
object RelaunchSupportedVersionFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/RelaunchActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(definingClass = "Li60;", name = "c")
    )
)

/**
 * MinecraftActivity.onCreate (:416-500) — Google Play kurulum denetimi:
 * getInstallerPackageName(MCPE) sonucu
 *   startsWith("com.android") && endsWith("ending") && contains(".v")
 *   && length == 19   (yani "com.android.vending")
 * ise prefs'e test="0" yazilir (Play kurulumu isareti).
 *
 * startsWith sonucunu 1'e zorlayarak zincir her zaman Play-yolunu secer —
 * MCPE nereden kurulursa kurulsun "Play'den yuklu" muamelesi gorur.
 */
object McInstallerCheckFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/MinecraftActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        string("com.android"),
        methodCall(definingClass = "Ljava/lang/String;", name = "startsWith")
    )
)

