package app.smashhit.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.smashhit.patches.shared.Constants.COMPATIBILITY_SMASHHIT

/**
 * Smash Hit Premium (Subscription Simulation)
 *
 * Simulates premium ownership by making ownsPremiumProduct() always
 * return true. This causes GooglePlaySystem.OnSyncCompleted() to skip
 * ad loading entirely.
 *
 * How it works:
 *
 * 1. AndroidStore.ownsPremiumProduct() → return true.
 *    The game thinks the user owns premium, so no ads are loaded.
 *
 * 2. CommandThreadsafeModel.hasRefreshedOwnedProducts() → return true.
 *    Skips the wait for purchase sync, premium is recognized immediately.
 */
@Suppress("unused")
val smashhitPremiumPatch = bytecodePatch(
    name = "Smash Hit Premium (Subscription Simulation)",
    description = "Simulates premium ownership to unlock ad-free experience.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SMASHHIT)

    execute {
        // 1. AndroidStore.ownsPremiumProduct() → return true
        OwnsPremiumProductFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 2. CommandThreadsafeModel.hasRefreshedOwnedProducts() → return true
        HasRefreshedOwnedProductsFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())
    }
}
