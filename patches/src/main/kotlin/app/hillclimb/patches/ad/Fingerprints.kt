package app.hillclimb.patches.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val MAIN_ACTIVITY = "Lcom/fingersoft/game/MainActivity;"

object HasVideoCampaignsFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "hasVideoCampaigns",
    returnType = "I",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("I"),
    filters = listOf(
        methodCall(definingClass = MAIN_ACTIVITY, name = "getAdsInstance"),
        methodCall(definingClass = "Lcom/fingersoft/game/firebase/CFirebaseAds;", name = "isRewardedVideoLoaded")
    )
)

object PlayRewardedVideoAdFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "playRewardedVideoAd",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Ljava/lang/String;", "I"),
    filters = listOf(
        methodCall(definingClass = MAIN_ACTIVITY, name = "getAdsInstance"),
        methodCall(definingClass = "Lcom/fingersoft/game/firebase/CFirebaseAds;", name = "showVideoAd")
    )
)

object SplashCompletedFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "splashScreenHasCompleted",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)

object LoadRewardedVideoFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "loadRewardedVideoFromGame",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)

object RewardedInterstitialLoadedFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "isRewardedInterstitialLoaded",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)

object IsShowingBannersFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "isShowingBanners",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)

object IsInterstitialLoadedGroupFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "isInterstitialLoadedGroup",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("I")
)

object IsFirebaseInitializedFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "isFirebaseInitialized",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)

object ShowRewardedInterstitialFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "showRewardedInterstitialFromGame",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)

object LogDonorFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "openAdmobAdInspector",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)