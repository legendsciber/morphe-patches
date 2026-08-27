package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium - Direct Enable
 *
 * Enables premium directly at startup by patching the premium state setter.
 * No ads, no watching required - premium is always active.
 *
 * How it works:
 *
 * 1. ya0.H(Z)V → always set Q=true without notifying observers.
 *    This is the premium state setter called throughout the app.
 *    By always setting Q=true, premium features are always enabled.
 *    F() notification is skipped to avoid BadTokenException in onCreate.
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. ya0.H(Z)V → always set Q=true, skip F() notification
        // .locals 1: v0 available
        // Original: if (p1 != Q) { Q = p1; F(); }
        // Patched: Q = true; return;
        // F() skipped because it triggers PopupWindow show in onCreate before window is ready
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())
    }
}
