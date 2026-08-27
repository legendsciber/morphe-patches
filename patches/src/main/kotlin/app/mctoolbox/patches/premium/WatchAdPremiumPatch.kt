package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium - Direct Enable
 *
 * Enables premium by patching only the ya0 premium state class.
 * All other app code (popups, overlays, UI) works normally.
 *
 * How it works:
 *
 * 1. ya0.<init>(Z)V → always set Q=true.
 *    The constructor creates premium state. By always setting Q=true,
 *    premium is active from the very first moment.
 *
 * 2. ya0.H(Z)V → always set Q=true, skip F() notification.
 *    The setter is called throughout the app. By always setting Q=true
 *    and skipping F() (data binding notification), premium stays active
 *    without triggering premature UI updates that cause crashes.
 *
 * This is the minimal 2-point patch that doesn't touch any UI code.
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. ya0.<init>(Z)V → always set Q=true
        // Insert const/4 p1, 0x1 before iput-boolean to override parameter
        PremiumInitFingerprint.method.addInstructions(1, """
            const/4 p1, 0x1
        """.trimIndent())

        // 2. ya0.H(Z)V → always set Q=true, skip F() notification
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())
    }
}
