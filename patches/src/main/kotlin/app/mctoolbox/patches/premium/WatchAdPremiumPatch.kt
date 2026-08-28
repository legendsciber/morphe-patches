package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium - Direct Enable
 *
 * ya0.H(Z)V → force Q=true, skip F().
 *
 * This enables premium state globally without triggering data binding
 * notifications (F()). The app's internal code checks ya0.Q for premium
 * features, so setting Q=true grants premium access.
 *
 * F() is skipped to prevent BadTokenException crashes when the app
 * tries to show premium popups/overlays during onCreate before the
 * window token is ready.
 *
 * Note: Premium UI elements (floating logo, etc.) may not display
 * until the app naturally calls ya0.H() after window is ready.
 * This is acceptable — premium functionality is active.
 *
 * Register safety: .locals 1 (v0) + p0 = 2 regs. Uses v0, p0 only.
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Force Q=true, skip F()
        // insert at 0 → before first instruction → original code unreachable
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())
    }
}
