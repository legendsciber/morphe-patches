package app.smashhit.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.smashhit.patches.shared.Constants.COMPATIBILITY_SMASHHIT

/**
 * Smash Hit Premium (Ad-Free Unlock)
 *
 * Simulates premium ownership at multiple levels to fully unlock
 * ad-free experience. The game checks premium status in several places:
 *
 * 1. AndroidStore.ownsPremiumProduct() - gates ad loading in OnSyncCompleted()
 * 2. CommandThreadsafeModel.isProductOwned() - core ownership check used by native engine
 * 3. GooglePlaySystem.OnSyncCompleted() - entry point for ad loading
 *
 * By patching all three, we ensure:
 * - No ads are loaded (OnSyncCompleted skips ad loading)
 * - Native engine thinks user owns premium (isProductOwned returns true)
 * - Game UI shows premium status
 */
@Suppress("unused")
val smashhitPremiumPatch = bytecodePatch(
    name = "Smash Hit Premium (Ad-Free Unlock)",
    description = "Simulates premium ownership to fully unlock ad-free experience.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SMASHHIT)

    execute {
        // 1. AndroidStore.ownsPremiumProduct() → return true
        // This is the master check used by OnSyncCompleted() to gate ad loading
        OwnsPremiumProductFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 2. CommandThreadsafeModel.isProductOwned() → return true
        // This is the core check used by native C++ engine via "isproductowned" command
        IsProductOwnedFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 3. GooglePlaySystem.OnSyncCompleted() → skip ad loading branch
        // Even if ownsPremiumProduct() is patched, we also skip the ad loading call directly
        // Original: calls LogHelper.breadcrumb(), then checks ownsPremiumProduct(), then loads ads
        // Patched: return immediately after breadcrumb (skip ad loading entirely)
        OnSyncCompletedFingerprint.method.addInstructionsWithLabels(0, """
            return-void
            nop
        """.trimIndent())
    }
}
