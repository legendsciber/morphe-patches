package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. Subscribe button bypass: n21.k() → directly activate premium
        //    Instead of launching Google Play billing, just call tc0.d(true)
        SubscribeBypassFingerprint.method.addInstructions(0, """
            check-cast p0, Lve0;
            iget-object v0, p0, Lve0;->b:Ltc0;
            const/4 v1, 0x1
            invoke-virtual {v0, v1}, Ltc0;->d(Z)V
            return-void
        """.trimIndent())

        // 2. Backup: tc0.d() always forces premium=true
        PremiumStateFingerprint.method.addInstructions(0, "const/4 p1, 0x1")
    }
}
