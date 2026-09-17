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
