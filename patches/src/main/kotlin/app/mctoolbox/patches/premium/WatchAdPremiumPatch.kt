package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium - Direct Enable
 *
 * Enables premium by patching ONLY the ya0 premium state class.
 * F() (data binding notification) is NEVER called, preventing
 * premature popup/overlay displays that cause BadTokenException.
 *
 * How it works:
 *
 * 1. ya0.<init>(Z)V → always set Q=true, no F() call.
 *    Constructor initializes premium state. By setting Q=true here,
 *    premium is active from the very first moment.
 *
 * 2. ya0.H(Z)V → always set Q=true, no F() call.
 *    Setter is called throughout the app. By always setting Q=true
 *    and never calling F(), no data binding callbacks are triggered,
 *    so no popups/overlays are shown prematurely.
 *
 * The app's own code that reads ya0.Q will see true (premium active).
 * The app's own code that calls ya0.H() goes through our patched
 * version which never calls F().
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. ya0.<init>(Z)V → set Q=true, skip F()
        // Original: iput-boolean p1, p0, Lya0;->Q:Z; return-void
        // Patched: const/4 p1, 0x1; iput-boolean p1, p0, Lya0;->Q:Z; return-void
        PremiumInitFingerprint.method.addInstructions(1, """
            const/4 p1, 0x1
        """.trimIndent())

        // 2. ya0.H(Z)V → set Q=true, skip F()
        // Original: if (p1 != Q) { Q = p1; F(); }
        // Patched: Q = true; return;
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())
    }
}
