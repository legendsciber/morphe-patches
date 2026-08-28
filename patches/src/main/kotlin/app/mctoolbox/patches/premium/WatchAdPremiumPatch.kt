package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium - Direct Enable
 *
 * Crash chain: xz0.<init> → ... → tz0.a() checks Q → shows overlay → crash
 *
 * Fix:
 * 1. ya0.H(Z)V → force Q=true, skip F() → premium features active
 * 2. tz0.a()V → goto :cond_0 → bypasses Q check, always runs normal path
 *    This prevents the premium overlay from being shown during init,
 *    while keeping all other data binding callbacks (toolbox UI, mod menu) working.
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. ya0.H() → Q=true, skip F()
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // 2. tz0.a() → always take the non-popup path
        // goto :cond_0 skips the Q check and the premium overlay Runnable
        PopupDecisionFingerprint.method.addInstructionsWithLabels(0, """
            goto :cond_0
        """.trimIndent())
    }
}
