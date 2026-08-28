package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium - Direct Enable
 *
 * Crash chain: xz0.<init> → ... → xs0.g() → popup.a() → showAtLocation → crash
 *
 * Fix:
 * 1. ya0.H(Z)V → force Q=true, skip F() → premium features active
 * 2. xs0.g(I, e)V → return-void → blocks ALL popup notification chains
 *    (jz0.a, rz0.a, mj.a, bz0.a etc. are never called)
 *
 * Premium features work via Q=true. Popups suppressed.
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

        // 2. xs0.g() → return-void (blocks ALL popup chains)
        DataBindingCallbackFingerprint.method.addInstructions(0, "return-void")
    }
}
