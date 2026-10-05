package app.hillclimb.patches.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val MAIN_ACTIVITY = "Lcom/fingersoft/game/MainActivity;"

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

object ShowRewardedInterstitialFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "showRewardedInterstitialFromGame",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
)
