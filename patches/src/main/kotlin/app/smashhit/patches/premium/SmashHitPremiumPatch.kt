package app.smashhit.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.smashhit.patches.shared.Constants.COMPATIBILITY_SMASHHIT

@Suppress("unused")
val smashhitPremiumPatch = bytecodePatch(
    name = "Smash Hit Premium Unlock",
    description = "Unlocks premium and all game modes without purchase.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SMASHHIT)

    execute {
        OwnsPremiumProductFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        IsProductOwnedFingerprint.method.addInstructions(1, """
            const/4 p1, 0x1
            monitor-exit p0
            return p1
        """.trimIndent())

        OnSyncCompletedFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        HasRefreshedOwnedProductsFingerprint.method.addInstructions(0, """
            const-string p1, "true"
            return-object p1
        """.trimIndent())

        IsPremiumProductRestoredFingerprint.method.addInstructions(0, """
            const-string p1, "true"
            return-object p1
        """.trimIndent())

        StartPurchaseFlowFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())
    }
}
