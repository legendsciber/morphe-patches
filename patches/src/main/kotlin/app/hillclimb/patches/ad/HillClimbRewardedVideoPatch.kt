package app.hillclimb.patches.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.hillclimb.patches.shared.Constants.COMPATIBILITY_HILLCLIMB

private const val MAIN_ACTIVITY = "Lcom/fingersoft/game/MainActivity;"
private const val ALOG = "Landroid/util/Log;"
private const val SVAL = "Ljava/lang/String;"

@Suppress("unused")
val hillClimbRewardedVideoPatch = bytecodePatch(
    name = "Hill Climb Racing Instant Rewarded Video Rewards",
    description = "Rewarded ads pay out instantly: the engine is told rewarded videos are available and receives the started and completed callbacks straight away, so no video plays and every reward is granted offline.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_HILLCLIMB)

    execute {
        SplashCompletedFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_PATCHED"
            invoke-static {v0, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent())

        HasVideoCampaignsFingerprint.method.addInstructions(0, """
            invoke-static {p0}, $SVAL->valueOf(I)$SVAL
            move-result-object p0
            invoke-static {p0, p0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
            const/16 p0, 0x3e8
            return p0
        """.trimIndent())

        PlayRewardedVideoAdFingerprint.method.addInstructions(0, """
            invoke-static {v1}, $SVAL->valueOf(I)$SVAL
            move-result-object v1
            invoke-static {v1, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
            invoke-static {}, $MAIN_ACTIVITY->onVideoStartedSuccess()V
            invoke-static {}, $MAIN_ACTIVITY->onVideoCompletedSuccess()V
            return-void
        """.trimIndent())
    }
}