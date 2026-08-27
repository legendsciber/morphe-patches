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
 *    F() notification is skipped to avoid BadTokenException in onCreate.
 *
 * 2-5. Premium popup classes → return immediately.
 *    Prevents PopupWindow.showAtLocation crashes in onCreate.
 *
 * 6. t20.run()V → post self to Handler with 200ms delay.
 *    The floating logo overlay uses WindowManager.addView which crashes
 *    if called before window token is ready. By posting with a delay,
 *    the window is ready when addView is called.
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

        // 6. t20.run()V → post to Handler with 200ms delay to avoid BadTokenException
        // Creates a new Handler, posts this Runnable with 200ms delay
        // This ensures WindowManager.addView is called after window is ready
        OverlayAddViewFingerprint.method.addInstructions(0, """
            new-instance v0, Landroid/os/Handler;
            invoke-direct {v0}, Landroid/os/Handler;-><init>()V
            const-wide v1, 0xc8L
            invoke-virtual {v0, p0, v1, v2}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
            return-void
        """.trimIndent())
    }
}
