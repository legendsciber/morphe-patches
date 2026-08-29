package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * 1. ya0.H() → always set Q=true on ALL instances → premium active everywhere
 * 2. tz0.a() → force xa0.c.Q = false at method entry → overlay always routes to normal Runnable
 * 3. Block popup crash paths (jz0, rz0, mj) → no BadTokenException
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. Enable premium: Q=true on every ya0 instance, skip F()
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // 2. Force overlay routing: set xa0.c.Q = false at tz0.a() entry
        //    This only affects the routing check — other ya0 instances keep Q=true
        PopupDecisionFingerprint.method.addInstructions(0, """
            iget-object v0, p0, Ltz0;->b:Ljava/lang/Object;
            check-cast v0, Lxa0;
            iget-object v0, v0, Lxa0;->c:Lya0;
            const/4 v1, 0x0
            iput-boolean v1, v0, Lya0;->Q:Z
        """.trimIndent())

        // 3. Block popup crash paths
        PopupJz0Fingerprint.method.addInstructions(0, "return-void")
        PopupRz0Fingerprint.method.addInstructions(0, "return-void")
        PopupMjFingerprint.method.addInstructions(0, "return-void")
    }
}
