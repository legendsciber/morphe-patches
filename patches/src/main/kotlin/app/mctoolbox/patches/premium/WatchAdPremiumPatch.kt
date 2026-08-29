package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * Two patches:
 * 1. ya0.H() - set Q=true directly (premium features enabled)
 * 2. xs0.g() - wrap in try-catch so BadTokenException during init is caught silently
 *    After init, when F() fires naturally, window is ready and popup shows
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Enable premium: ya0.H() → always set Q=true, skip F()
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // Wrap xs0.g() in try-catch to catch BadTokenException during init
        // xs0 is NOT synthetic, so addInstructionsWithLabels should work
        Xs0TriggerFingerprint.method.addInstructionsWithLabels(0, """
            :try_start
            iget-object p0, p0, Lxs0;->a:Lxs0${'$'}a;
            invoke-interface {p0}, Lxs0${'$'}a;->a()V
            :try_end
            .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch
            return-void
            :catch
            move-exception p0
            return-void
        """.trimIndent())
    }
}
