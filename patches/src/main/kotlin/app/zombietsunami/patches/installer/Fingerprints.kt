package app.zombietsunami.patches.installer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

private const val LICENSE_CLIENT = "Lcom/pairip/licensecheck/LicenseClient;"

object LocalInstallerCheckFingerprint : Fingerprint(
    definingClass = LICENSE_CLIENT,
    name = "performLocalInstallerCheck",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(smali = "Landroid/content/pm/InstallSourceInfo;->getInstallingPackageName()Ljava/lang/String;"),
    ),
)
