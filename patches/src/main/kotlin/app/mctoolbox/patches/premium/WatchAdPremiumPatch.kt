package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
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
 * 2-6. All premium popup/overlay classes → return immediately or skip.
 *    When premium is active, the app tries to show popup windows
 *    and overlays via showAtLocation/addView in onCreate, but window
 *    token is null. By skipping all displays, premium is active
 *    without crashes.
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

        // 2. jz0.a()V → skip popup
        PremiumPopupFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        // 3. rz0.a()V → skip popup
        PremiumPopup2Fingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        // 4. mj.a()V → skip popup
        PremiumPopup3Fingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        // 5. bz0.run()V → skip popup
        PremiumPopup4Fingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        // 6. tz0.a()V → skip premium overlay trigger
        // This method checks ya0.Q and runs the overlay Runnable
        // that calls WindowManager.addView. By returning immediately,
        // the overlay is never shown.
        PremiumOverlayTriggerFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())
    }
}
