package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * 1. ya0.H() → always set Q=true, skip F() → premium active
 * 2. Block popup crash paths (jz0, rz0, mj) → return-void
 * 3. Force tz0.a() to always route to normal Runnable (overlay)
 *    by inserting const/4 v1, 0x0 before the if-eqz check on Q
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Enable premium: Q=true, skip F()
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // Force overlay routing: override Q check in tz0.a() → always take normal Runnable path
        PopupDecisionFingerprint.method.addInstructions(11, "const/4 v1, 0x0")

        // Block popup crash paths
        PopupJz0Fingerprint.method.addInstructions(0, "return-void")
        PopupRz0Fingerprint.method.addInstructions(0, "return-void")
        PopupMjFingerprint.method.addInstructions(0, "return-void")
    }
}
