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

object GetApplicationInstallerFingerprint : Fingerprint(
    definingClass = "Lorg/naturalmotion/NmgSystem/NmgMarketplace;",
    name = "GetApplicationInstaller",
    returnType = "I",
    filters = listOf(
        string("PackageManager.getInstallerPackageName aka Market name: ")
    )
)
