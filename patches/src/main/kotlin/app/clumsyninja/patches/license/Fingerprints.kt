package app.clumsyninja.patches.license

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

object DontAllowFingerprint : Fingerprint(
    definingClass = "Lorg/naturalmotion/NmgSystem/NmgMarketplaceGooglePlayApkExpansion\$1;",
    name = "dontAllow",
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        string("Authentication rejected.")
    )
)

object ApplicationErrorFingerprint : Fingerprint(
    definingClass = "Lorg/naturalmotion/NmgSystem/NmgMarketplaceGooglePlayApkExpansion\$1;",
    name = "applicationError",
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        string("ERROR_INVALID_PACKAGE_NAME")
    )
)

object GetApplicationInstallerFingerprint : Fingerprint(
    definingClass = "Lorg/naturalmotion/NmgSystem/NmgMarketplace;",
    name = "GetApplicationInstaller",
    returnType = "I",
    filters = listOf(
        string("PackageManager.getInstallerPackageName aka Market name: ")
    )
)
