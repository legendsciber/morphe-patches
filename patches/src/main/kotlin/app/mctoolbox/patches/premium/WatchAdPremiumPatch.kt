package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * 1. ya0.H(Z)V → force Q=true, skip F() → premium features active
 * 2. jz0/rz0/mj.a() → return-void → block premium popup crashes
 * 3. t20.run() → Handler.postDelayed(self, 500ms) → delays overlay
 *    until window is ready (prevents BadTokenException)
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Premium state: Q=true, skip F()
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // Block premium popup crashes
        PopupJz0Fingerprint.method.addInstructions(0, "return-void")
        PopupRz0Fingerprint.method.addInstructions(0, "return-void")
        PopupMjFingerprint.method.addInstructions(0, "return-void")

        // Delay overlay: t20.run() posts itself to Handler with 500ms delay
        // .locals 5: v0-v4 + p0 = 6 regs
        OverlayShowFingerprint.method.addInstructions(0, """
            new-instance v0, Landroid/os/Handler;
            invoke-direct {v0}, Landroid/os/Handler;-><init>()V
            const-wide v1, 0x1f4L
            invoke-virtual {v0, p0, v1, v2}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
            return-void
        """.trimIndent())
    }
}
