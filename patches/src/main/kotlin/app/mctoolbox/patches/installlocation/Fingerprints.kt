package app.mctoolbox.patches.installlocation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * MinecraftActivity.onCreate — ilk talimat: const-string "com.mojang.minecraftpe".
 * getPackageInfo bu paketle basarisiz olursa :catch_2 -> "not_installed" hatasi.
 * Literal metodda yalnizca bir kez gecer.
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
 * RelaunchActivity.onCreate — ayni kalip (smali :233):
 * const-string v3 "com.mojang.minecraftpe" -> NameNotFoundException -> "not_installed".
 */
object RelaunchPackageLookupFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/RelaunchActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        string("com.mojang.minecraftpe")
    )
)

/**
 * MinecraftActivity.onCreate (:533) — genel surum kapisi.
 * Ilk Li60.c(versionName,true) sonucu 0 ise ikinci c() ve "not_supported" hatasi.
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
 * RelaunchActivity.onCreate — ayni genel surum kapisi.
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
 * La0.b(ApplicationInfo)Z — "MCPE 64-bit mi?" kontrolu.
 * MinecraftActivity.onCreate icinde IKI cagri noktasi (:634 ve :736):
 *   - true  => 32-bit Toolbox + 64-bit MCPE uyarisi VEYA 64-bit yeniden baslatma
 *   - false => mimari uyumlu kabul edilir, hicbir ABI hatasi/relaunch olmaz
 *
 * Her iki cagrinin sonucu 0'a zorlanir; boylece Toolbox calistigi bitlikte
 * devam eder. Dikkat: gercek mimari uyusmazliginda native crash mumkun —
 * bu yama varsayilan olarak KAPALI gelir.
 */
object McAbiRelaunchFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/MinecraftActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    accessFlags = listOf(AccessFlags.PUBLIC),
    filters = listOf(
        methodCall(definingClass = "La0;", name = "b")
    )
)

/**
 * MinecraftActivity.onCreate (:416-500) — Google Play kurulum denetimi:
 * getInstallerPackageName(MCPE) sonucu
 *   startsWith("com.android") && endsWith("ending") && contains(".v")
 *   && length == 19   (yani "com.android.vending")
 * ise prefs'e test="0" yazilir (Play kurulumu isareti).
 *
 * startsWith sonucunu 1'e zorlayarak zincir her zaman Play-yolunu secer.
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
