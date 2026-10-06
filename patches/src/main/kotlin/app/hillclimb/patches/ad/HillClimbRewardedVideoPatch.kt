package app.hillclimb.patches.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.hillclimb.patches.shared.Constants.COMPATIBILITY_HILLCLIMB

private const val MAIN_ACTIVITY = "Lcom/fingersoft/game/MainActivity;"
private const val LOG = "Lcom/fingersoft/utils/Log;"

@Suppress("unused")
val hillClimbRewardedVideoPatch = bytecodePatch(
    name = "Hill Climb Racing Instant Rewarded Video Rewards",
    description = "Rewarded ads pay out instantly: no video plays, the engine receives the started and completed callbacks straight away so the reward is granted offline too.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_HILLCLIMB)

    execute {
        PlayRewardedVideoAdFingerprint.method.addInstructions(0, """
            const-string v1, "hcr"
            invoke-static {v1, v0}, $LOG->d(Ljava/lang/String;Ljava/lang/String;)V
            invoke-static {}, $MAIN_ACTIVITY->onVideoStartedSuccess()V
            invoke-static {}, $MAIN_ACTIVITY->onVideoCompletedSuccess()V
            return-void
        """.trimIndent())

        ShowRewardedInterstitialFingerprint.method.addInstructions(0, """
            invoke-static {}, $MAIN_ACTIVITY->isShowingBanners()Z
            return-void
        """.trimIndent())

        IsShowingBannersFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_SHOW_RI"
            invoke-static {v0, v0}, $LOG->d(Ljava/lang/String;Ljava/lang/String;)V
        """.trimIndent())

        RewardedInterstitialLoadedFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_IS_RI"
            invoke-static {v0, v0}, $LOG->d(Ljava/lang/String;Ljava/lang/String;)V
        """.trimIndent())
    }
}