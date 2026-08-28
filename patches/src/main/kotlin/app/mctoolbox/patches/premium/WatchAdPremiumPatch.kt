package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium - Direct Enable
 *
 * Crash chain: xz0.<init> → mz0.g → xs0.g → jz0.a → showAtLocation → crash
 *
 * Fix: Two patches:
 * 1. ya0.H(Z)V → force Q=true, skip F() → premium features active
 * 2. jz0.a()V → return-void → prevents popup showAtLocation crash
 *
 * Premium features work via Q=true. The premium popup/overlay is suppressed
 * to prevent BadTokenException during onCreate when window token is null.
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

        // 2. jz0.a() → return-void (prevent showAtLocation crash)
        PremiumPopupFingerprint.method.addInstructions(0, "return-void")
    }
}
