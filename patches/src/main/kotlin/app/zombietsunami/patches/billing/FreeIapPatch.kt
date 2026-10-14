package app.zombietsunami.patches.billing

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zombietsunami.patches.shared.Constants.COMPATIBILITY_ZOMBIETSUNAMI

/**
 * Zombie Tsunami is Cocos2d-x, not Unity IL2CPP: the game logic lives in the stripped
 * `libcgame.so` while billing is entirely on the Java side. The engine is handed entitlements
 * through exactly four JNI callbacks - `nativeOnPurchaseSucceed`, `nativeOnPurchaseFailed`,
 * `nativeOnRestorePurchase` and `nativeOnPurchaseVerifiedForTracking` - and there is no revoke
 * counterpart, so Java can only ever add an entitlement. Once `nativeOnPurchaseSucceed` fires,
 * the product stays owned in the game's own save and a later `refreshPurchasesAsync` cannot take
 * it away.
 *
 * The patch therefore short-circuits the one funnel every purchase goes through. Granting from
 * there instead of from `onPurchaseSucceed` produces the identical engine callback with the
 * identical sku, just without the Google Play round trip.
 *
 * Coin packs are consumable, but nothing is ever bought on Google's side, so
 * `consumeInappPurchase` has no purchase to consume and is a harmless no-op: tapping buy again
 * simply grants again.
 */
@Suppress("unused")
val zombieTsunamiFreeIapPatch = bytecodePatch(
    name = "Zombie Tsunami Free IAP",
    description = "Every store item is granted the moment you tap buy, with no Google Play billing sheet in between: all coin packs, the coins and diamonds bundle and the double coins upgrade arrive instantly and for free, and coin packs can be bought as many times as you like.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ZOMBIETSUNAMI)

    execute {
        // The anchor is the `m_BillingClient` read at `:cond_0`; the hook goes one instruction
        // below it, so it is reached by fall-through from the non-null sku path. Landing on the
        // labelled instruction itself instead would leave the hook at the mercy of how the
        // assembler re-binds `:cond_0` - and one perfectly legal reading makes the branch leap
        // straight over it. Index+1 carries no label, so no such reading exists.
        LaunchBillingFlowFingerprint.method.addInstructions(
            LaunchBillingFlowFingerprint.instructionMatches[1].index + 1,
            """
                invoke-static {p2}, Lnet/mobigame/artemis/billing/MobiGooglePlayBilling;->nativeOnPurchaseSucceed(Ljava/lang/String;)V
                return-void
            """.trimIndent(),
        )
    }
}
