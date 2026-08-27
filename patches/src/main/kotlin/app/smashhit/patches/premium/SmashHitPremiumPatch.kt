package app.smashhit.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.smashhit.patches.shared.Constants.COMPATIBILITY_SMASHHIT

/**
 * Smash Hit Premium (Ad-Free Unlock)
 *
 * Simulates premium ownership at multiple levels to fully unlock
 * ad-free experience and handle purchase flow gracefully.
 *
 * How it works:
 *
 * 1. AndroidStore.ownsPremiumProduct() → return true.
 *    Gates ad loading in OnSyncCompleted().
 *
 * 2. CommandThreadsafeModel.isProductOwned() → return true.
 *    Core ownership check used by native C++ engine.
 *
 * 3. GooglePlaySystem.OnSyncCompleted() → return immediately.
 *    Skips ad loading entry point entirely.
 *
 * 4. AndroidStore.startPurchaseFlow() → return immediately.
 *    Prevents Play Store from opening when user tries to buy premium.
 *
 * 5. storegetstatus → return "0" (PURCHASE_OK).
 *    Tells native engine purchase completed successfully.
 *
 * 6. storegeterror → return "0" (no error).
 *    Tells native engine no purchase error occurred.
 */
@Suppress("unused")
val smashhitPremiumPatch = bytecodePatch(
    name = "Smash Hit Premium (Ad-Free Unlock)",
    description = "Simulates premium ownership to fully unlock ad-free experience and handle purchase flow.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SMASHHIT)

    execute {
        // 1. AndroidStore.ownsPremiumProduct() → return true
        OwnsPremiumProductFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 2. CommandThreadsafeModel.isProductOwned() → return true
        IsProductOwnedFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 3. GooglePlaySystem.OnSyncCompleted() → return immediately (skip ad loading)
        OnSyncCompletedFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        // 4. AndroidStore.startPurchaseFlow() → return immediately (prevent Play Store)
        StartPurchaseFlowFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        // 5. storegetstatus → return "0" (PURCHASE_OK)
        StoreGetStatusFingerprint.method.addInstructions(0, """
            const-string v0, "0"
            return-object v0
        """.trimIndent())

        // 6. storegeterror → return "0" (no error)
        StoreGetErrorFingerprint.method.addInstructions(0, """
            const-string v0, "0"
            return-object v0
        """.trimIndent())
    }
}
