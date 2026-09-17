package app.clumsyninja.patches.permission

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.clumsyninja.patches.shared.Constants.COMPATIBILITY_CLUMSYNINJA

@Suppress("unused")
val clumsyNinjaPermissionPatch = bytecodePatch(
    name = "Storage Permission Bypass",
    description = "Bypasses storage permission check so the game proceeds without asking.",
    default = true
) {
    compatibleWith(COMPATIBILITY_CLUMSYNINJA)

    execute {
        GetApplicationInstallerFingerprint.method.addInstructions(0, """
            const/4 v0, 0x3
            return v0
        """.trimIndent())

        QueryPermissionFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())
    }
}
