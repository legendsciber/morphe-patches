package app.hillclimb.patches.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.hillclimb.patches.shared.Constants.COMPATIBILITY_HILLCLIMB
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

object RewardedFrequencyCapFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "lambda\$initialiseAdvertising\$16",
    returnType = "V",
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.SYNTHETIC),
    parameters = listOf("[Ljava/lang/String;", "Ljava/lang/String;", "I", "[Ljava/lang/String;", "[Ljava/lang/String;"),
    filters = listOf(
        methodCall(definingClass = MAIN_ACTIVITY, name = "createRewardedInterstitialFrequencyCap")
    )
)