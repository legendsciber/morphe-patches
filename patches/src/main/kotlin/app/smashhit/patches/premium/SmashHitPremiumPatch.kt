package app.smashhit.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.smashhit.patches.shared.Constants.COMPATIBILITY_SMASHHIT

/**
 * Smash Hit Premium (Ad-Free Unlock)
 *
 * Simulates premium ownership at multiple levels to fully unlock
 * ad-free experience and prevent Play Store purchase attempts.
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
 */
@Suppress("unused")
val smashhitPremiumPatch = bytecodePatch(
    name = "Smash Hit Premium (Ad-Free Unlock)",
    description = "Simulates premium ownership to fully unlock ad-free experience and prevent Play Store purchase.",
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
    }
}
