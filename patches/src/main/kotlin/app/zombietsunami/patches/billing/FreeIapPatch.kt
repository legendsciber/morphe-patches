package app.zombietsunami.patches.billing

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zombietsunami.patches.shared.Constants.COMPATIBILITY_ZOMBIETSUNAMI

@Suppress("unused")
val zombieTsunamiFreeIapPatch = bytecodePatch(
    name = "Zombie Tsunami Free IAP",
    description = "Every store item is granted the moment you tap buy, with no Google Play billing sheet in between: all coin packs, the coins and diamonds bundle and the double coins upgrade arrive instantly and for free, and coin packs can be bought as many times as you like.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ZOMBIETSUNAMI)

    execute {
        LaunchBillingFlowFingerprint.method.addInstructions(
            LaunchBillingFlowFingerprint.instructionMatches[1].index + 1,
            """
                invoke-static {p2}, Lnet/mobigame/artemis/billing/MobiGooglePlayBilling;->nativeOnPurchaseSucceed(Ljava/lang/String;)V
                return-void
            """.trimIndent(),
        )
    }
}
