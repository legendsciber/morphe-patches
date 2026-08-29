package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (IAP Bypass)
 *
 * Instead of manipulating Q directly, we bypass the premium state check:
 * 1. tc0.d(boolean) → always force p1=true → premium state always active
 * 2. This propagates through bridge → ya0.H(true) → Q=true naturally
 * 3. Overlay routing: tz0.a() forces xa0.c.Q=false → overlay shows despite Q=true
 * 4. mz0.g() skip → prevent init-time crash
 * 5. jz0/rz0/mj blocks → prevent popup BadTokenException
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // IAP Bypass: tc0.d() always sets premium=true
        PremiumStateFingerprint.method.addInstructions(0, "const/4 p1, 0x1")

        // Force overlay routing despite Q=true
        PopupDecisionFingerprint.method.addInstructions(0, """
            iget-object v0, p0, Ltz0;->b:Ljava/lang/Object;
            check-cast v0, Lxa0;
            iget-object v0, v0, Lxa0;->c:Lya0;
            const/4 v1, 0x0
            iput-boolean v1, v0, Lya0;->Q:Z
        """.trimIndent())

        // Prevent init-time trigger from mz0.g()
        Mz0TriggerFingerprint.method.addInstructions(11, "return-void")

        // Block popup crash paths
        PopupJz0Fingerprint.method.addInstructions(0, "return-void")
        PopupRz0Fingerprint.method.addInstructions(0, "return-void")
        PopupMjFingerprint.method.addInstructions(0, "return-void")
    }
}
