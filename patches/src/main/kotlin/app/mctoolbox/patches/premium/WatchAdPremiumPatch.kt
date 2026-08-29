package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * Two-phase approach to avoid BadTokenException:
 *
 * Phase 1 - During onCreate (init):
 *   mz0.g(ya0): skip xs0.g() trigger → callback registered but NOT fired → no crash
 *
 * Phase 2 - During onResume (window ready):
 *   1. Set ya0.Q = true → premium features active
 *   2. Trigger xs0.g(0, null) → tz0.a() → Runnable → popup/overlay shows → window ready → no crash
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Phase 1: mz0.g(ya0) - defer the xs0.g() trigger
        // Insert return-void before the invoke-virtual to xs0.g(ILandroidx/databinding/e;)V
        // This skips the callback trigger during init
        Mz0TriggerFingerprint.method.addInstructions(11, "return-void")

        // Phase 2: onResume() - trigger everything after window is ready
        // Insert before return-void (index 5):
        // 1. Set ya0.Q = true via mz0.d
        // 2. Trigger xs0.g(0, null) via mz0.e
        OnResumeFingerprint.method.addInstructions(5, """
            iget-object v0, p0, Lio/mrarm/mctoolbox/MinecraftActivity;->X:Lxz0;
            iget-object v0, v0, Lxz0;->g:Lmz0;
            iget-object v0, v0, Lmz0;->d:Lya0;
            const/4 v1, 0x1
            iput-boolean v1, v0, Lya0;->Q:Z
            iget-object v0, p0, Lio/mrarm/mctoolbox/MinecraftActivity;->X:Lxz0;
            iget-object v0, v0, Lxz0;->g:Lmz0;
            iget-object v0, v0, Lmz0;->e:Lxs0;
            const/4 v1, 0x0
            invoke-virtual {v0, v1, v1}, Lxs0;->g(ILandroidx/databinding/e;)V
        """.trimIndent())
    }
}
