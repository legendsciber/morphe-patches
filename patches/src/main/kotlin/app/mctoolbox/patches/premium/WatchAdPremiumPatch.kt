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
        SubscribeBypassFingerprint.method.addInstructions(0, """
            move-object/from16 v0, p0
            check-cast v0, Lve0;
            iget-object v0, v0, Lve0;->b:Ltc0;
            const/4 v1, 0x1
            invoke-virtual {v0, v1}, Ltc0;->d(Z)V
            return-void
        """.trimIndent())
    }
}
