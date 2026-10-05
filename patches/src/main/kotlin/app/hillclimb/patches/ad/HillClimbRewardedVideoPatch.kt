package app.hillclimb.patches.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.hillclimb.patches.shared.Constants.COMPATIBILITY_HILLCLIMB

private const val MAIN_ACTIVITY = "Lcom/fingersoft/game/MainActivity;"

@Suppress("unused")
val hillClimbRewardedVideoPatch = bytecodePatch(
    name = "Hill Climb Racing Instant Rewarded Video Rewards",
    description = "Rewarded ads pay out instantly: no video plays, the engine receives the started and completed callbacks straight away so the reward is granted offline too.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_HILLCLIMB)

    execute {
        PlayRewardedVideoAdFingerprint.method.addInstructions(0, """
            invoke-static {}, $MAIN_ACTIVITY->onVideoStartedSuccess()V
            invoke-static {}, $MAIN_ACTIVITY->onVideoCompletedSuccess()V
            return-void
        """.trimIndent())

        ShowRewardedInterstitialFingerprint.method.addInstructions(0, """
            invoke-static {}, $MAIN_ACTIVITY->onVideoStartedSuccess()V
            invoke-static {}, $MAIN_ACTIVITY->onVideoCompletedSuccess()V
            return-void
        """.trimIndent())
    }
}