package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * Key insight: F() MUST fire for data binding to update.
 * If F() is skipped, tz0.a() never gets called, overlay never shows.
 *
 * 1. ya0.H() → set Q=true, let F() fire → data binding triggers tz0.a()
 * 2. tz0.a() → force xa0.c.Q = false → routes to overlay Runnable (xa0.b)
 * 3. mz0.g() → skip trigger → prevent init-time crash from that path
 * 4. jz0/rz0/mj → return-void → block popup crashes
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. Set Q=true, DON'T skip F() — data binding must fire
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // 2. Force overlay routing: xa0.c.Q = false at tz0.a() entry
        PopupDecisionFingerprint.method.addInstructions(0, """
            iget-object v0, p0, Ltz0;->b:Ljava/lang/Object;
            check-cast v0, Lxa0;
            iget-object v0, v0, Lxa0;->c:Lya0;
            const/4 v1, 0x0
            iput-boolean v1, v0, Lya0;->Q:Z
        """.trimIndent())

        // 3. Prevent init-time trigger from mz0.g()
        Mz0TriggerFingerprint.method.addInstructions(11, "return-void")

        // 4. Block popup crash paths
        PopupJz0Fingerprint.method.addInstructions(0, "return-void")
        PopupRz0Fingerprint.method.addInstructions(0, "return-void")
        PopupMjFingerprint.method.addInstructions(0, "return-void")
    }
}
