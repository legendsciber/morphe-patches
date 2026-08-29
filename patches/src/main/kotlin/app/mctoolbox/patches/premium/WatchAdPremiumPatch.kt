package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * 1. ya0.H(Z)V → force Q=true, skip F() → premium features active
 * 2. jz0.a() / rz0.a() / mj.a() / bz0.a() → return-void
 *    Blocks ALL PopupWindow.showAtLocation crash paths during onCreate.
 *    Toolbox UI and mod menu are NOT affected (they use different callbacks).
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Premium state: Q=true, skip F()
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // Block ALL popup crash paths
        PopupJz0Fingerprint.method.addInstructions(0, "return-void")
        PopupRz0Fingerprint.method.addInstructions(0, "return-void")
        PopupMjFingerprint.method.addInstructions(0, "return-void")
        PopupBz0Fingerprint.method.addInstructions(0, "return-void")
    }
}
