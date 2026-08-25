package app.mctoolbox.patches.installlocation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * MinecraftActivity.onCreate (:533) — genel surum kapisi.
 * Ilk Li60.c(versionName,true) sonucu 0 ise ikinci c() ve "not_supported" hatasi.
 *
 * Bu filtre metodun TUM Li60.c cagrilariyla eslesir; kullanim YALNIZCA
 * instructionMatches[0] (ilk site) uzerindedir. Diger siteler:
 *   - :655 semantigi TERS (true -> hata uretir) — dokunulmaz
 *   - :801/:875 McAbiVersionGateFingerprint ile yonetilir
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
 * MinecraftActivity.onCreate — iki ABI surum kapisi (smali :801 ve :875),
 * tam kalip:
 *
 *   invoke-static {}, La0;->a()Z              (Toolbox sureci 64-bit mi?)
 *   iget-object ... ->V:Landroid/content/pm/PackageInfo;
 *   iget-object ... ->versionName:Ljava/lang/String;
 *   invoke-virtual ..., Li60;->c(...)Z        <- 0 donerse
 *                                               not_supported_64bit / _32bit
 *
 * Onceki surum kapilarinin (:533, :655) onlerinde La0.a() cagrisi OLMADIGI
 * icin bu 4-filtre zinciri TAM OLARAK bu iki siteye uyar.
 *
 * Gercek MCPE PackageInfo'su sorgulandigi icin bu kapilar gercek surume gore
 * degerlendirilir; sonucu 1'e zorlamak listede olmayan her surumu kabul eder.
 */
object McAbiVersionGateFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/MinecraftActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(definingClass = "La0;", name = "a"),
        fieldAccess(smali = "Lio/mrarm/mctoolbox/MinecraftActivity;->V:Landroid/content/pm/PackageInfo;"),
        fieldAccess(smali = "Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;"),
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
