package app.smashhit.patches.ad

import app.morphe.patcher.Fingerprint

/**
 * AdMob.smali - Registers an ad unit as active and triggers loading.
 * confirmShouldShowAds(AdUnit)V adds the unit to mActiveAdUnits and calls tryToLoadAds().
 * Body is replaced: always returns immediately (no ads registered).
 */
object ConfirmShouldShowAdsFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AdMob;",
    name = "confirmShouldShowAds",
    returnType = "V",
    parameters = listOf("Lcom/mediocre/smashhit/AdUnitDefinitions\$AdUnit;")
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
    parameters = listOf()
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
    parameters = listOf("Lcom/mediocre/smashhit/AdUnitDefinitions\$AdUnit;")
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
    parameters = listOf()
)
