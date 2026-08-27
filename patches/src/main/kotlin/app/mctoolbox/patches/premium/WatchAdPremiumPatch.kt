package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium - Direct Enable
 *
 * Enables premium directly at startup.
 * No ads, no watching required - premium is always active.
 *
 * How it works:
 *
 * 1. ya0.H(Z)V → always set Q=true without notifying observers.
 *    This is the premium state setter called throughout the app.
 *    F() notification is skipped to avoid BadTokenException in onCreate.
 *
 * 2. jz0.a()V → return immediately (skip popup).
 *    This method shows a PopupWindow when premium is active.
 *    It crashes in onCreate because window token is null.
 *    By skipping it, premium is still active but no popup is shown.
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
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // 2. jz0.a()V → skip popup (prevents BadTokenException crash)
        PremiumPopupFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())
    }
}
