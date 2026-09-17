package app.clumsyninja.patches.permission

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

object QueryPermissionFingerprint : Fingerprint(
    definingClass = "Lorg/naturalmotion/NmgSystem/NmgPermissions;",
    name = "QueryPermission",
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    filters = listOf(
        string("QueryPermission: ")
    )
)

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
