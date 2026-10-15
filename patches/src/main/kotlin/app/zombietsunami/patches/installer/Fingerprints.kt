package app.zombietsunami.patches.installer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

private const val LICENSE_CLIENT = "Lcom/pairip/licensecheck/LicenseClient;"

/**
 * The one installer check that actually gates the game.
 *
 * `com.pairip.application.Application` is the `android:name` of the `<application>` tag, so its
 * `attachBaseContext` runs before anything else and calls `LicenseClient.checkLicense`, which
 * enables the local check by default (`localCheckEnabled` is initialised to `true`) and runs
 * [performLocalInstallerCheck] on every launch. The method ends by comparing
 * `InstallSourceInfo.getInstallingPackageName()` against `"com.android.vending"` and returning
 * false - which never clears the state the rest of the flow needs - on anything else.
 *
 * The APK holds four call sites of `getInstallingPackageName` in total, and only this one decides
 * anything: the other three sit in Adjust, AppLovin and the Google ads SDK, where they merely
 * report the install source for attribution. Pinning the fingerprint to the method rather than to
 * the platform call is what keeps the patch down to the single site that matters.
 */
object LocalInstallerCheckFingerprint : Fingerprint(
    definingClass = LICENSE_CLIENT,
    name = "performLocalInstallerCheck",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            smali = "Landroid/content/pm/InstallSourceInfo;->getInstallingPackageName()Ljava/lang/String;",
        ),
    ),
)
