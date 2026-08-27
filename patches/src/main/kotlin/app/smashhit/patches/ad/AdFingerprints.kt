package app.smashhit.patches.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * AdMob.smali - Registers an ad unit as active and triggers loading.
 * confirmShouldShowAds(AdUnit)V adds the unit to mActiveAdUnits and calls tryToLoadAds().
 * Body is replaced: always returns immediately (no ads registered).
 */
object ConfirmShouldShowAdsFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AdMob;",
    name = "confirmShouldShowAds",
    returnType = "V",
    parameters = listOf("Lcom/mediocre/smashhit/AdUnitDefinitions\$AdUnit;"),
    filters = listOf(
        string("confirmShouldShowAds")
    )
)

/**
 * AdRewardedPool.smali - Shows a loaded rewarded ad from the pool.
 * showAd()Z iterates the pool in reverse to find and show a loaded ad.
 * Body is replaced: always returns false (no ad shown).
 */
object ShowAdPoolFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AdRewardedPool;",
    name = "showAd",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("firebase.test.lab")
    )
)

/**
 * AdMob.smali - Shows a loaded rewarded ad for the given unit.
 * showAd(AdUnit)V delegates to AdRewardedPool.showAd().
 * Body is replaced: always returns immediately.
 */
object ShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AdMob;",
    name = "showAd",
    returnType = "V",
    parameters = listOf("Lcom/mediocre/smashhit/AdUnitDefinitions\$AdUnit;"),
    filters = listOf(
        string("smashhit")
    )
)

/**
 * AdMob.smali - Entry point for ad initialization.
 * managePrivateConsentAndLoadAds()V requests UMP consent then initializes ads.
 * Body is replaced: always returns immediately (no ad SDK init).
 */
object ManageConsentAndLoadAdsFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AdMob;",
    name = "managePrivateConsentAndLoadAds",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("android_id")
    )
)
