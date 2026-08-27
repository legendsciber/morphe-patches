package app.smashhit.patches.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.smashhit.patches.shared.Constants.COMPATIBILITY_SMASHHIT

/**
 * Smash Hit Ad Removal
 *
 * Removes all rewarded video ads by disabling ad registration, display,
 * and SDK initialization.
 *
 * How it works:
 *
 * 1. AdMob.confirmShouldShowAds() → return immediately.
 *    Prevents ad units from being registered (no ads will load).
 *
 * 2. AdMob.showAd() → return immediately.
 *    Prevents any ad from being displayed.
 *
 * 3. AdRewardedPool.showAd() → return false.
 *    Even if ads are loaded, they cannot be shown.
 *
 * 4. AdMob.managePrivateConsentAndLoadAds() → return immediately.
 *    Prevents AdMob SDK initialization and consent flow.
 */
@Suppress("unused")
val smashhitAdRemovalPatch = bytecodePatch(
    name = "Smash Hit Ad Removal",
    description = "Removes all rewarded video ads (checkpoint and out-of-balls ads).",
    default = true
) {
    compatibleWith(COMPATIBILITY_SMASHHIT)

    execute {
        // 1. AdMob.confirmShouldShowAds() → return immediately
        ConfirmShouldShowAdsFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        // 2. AdMob.showAd() → return immediately
        ShowAdFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())

        // 3. AdRewardedPool.showAd() → return false
        ShowAdPoolFingerprint.method.addInstructions(0, """
            const/4 v0, 0x0
            return v0
        """.trimIndent())

        // 4. AdMob.managePrivateConsentAndLoadAds() → return immediately
        ManageConsentAndLoadAdsFingerprint.method.addInstructions(0, """
            return-void
        """.trimIndent())
    }
}
