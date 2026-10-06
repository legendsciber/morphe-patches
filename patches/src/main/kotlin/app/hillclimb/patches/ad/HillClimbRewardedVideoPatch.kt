package app.hillclimb.patches.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.hillclimb.patches.shared.Constants.COMPATIBILITY_HILLCLIMB

private const val MAIN_ACTIVITY = "Lcom/fingersoft/game/MainActivity;"
private const val ALOG = "Landroid/util/Log;"

@Suppress("unused")
val hillClimbRewardedVideoPatch = bytecodePatch(
    name = "Hill Climb Racing Instant Rewarded Video Rewards",
    description = "Rewarded ads pay out instantly: the engine is told a rewarded video is available and receives the started and completed callbacks straight away, so no video plays and every reward is granted offline.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_HILLCLIMB)

    execute {
        SplashCompletedFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_PATCHED"
            invoke-static {v0, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent())

        HasVideoCampaignsFingerprint.method.addInstructions(0, """
            const-string p0, "HCR_HVC"
            invoke-static {p0, p0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
            const/4 p0, 0x1
            return p0
        """.trimIndent())

        PlayRewardedVideoAdFingerprint.method.addInstructions(0, """
            const-string v1, "hcr"
            invoke-static {v1, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
            invoke-static {}, $MAIN_ACTIVITY->onVideoStartedSuccess()V
            invoke-static {}, $MAIN_ACTIVITY->onVideoCompletedSuccess()V
            return-void
        """.trimIndent())

        ShowRewardedInterstitialFingerprint.method.addInstructions(0, """
            invoke-static {}, $MAIN_ACTIVITY->openAdmobAdInspector()V
            return-void
        """.trimIndent())

        LogDonorFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_SHOW_RI"
            invoke-static {v0, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
            return-void
        """.trimIndent())

        RewardedInterstitialLoadedFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_IS_RI"
            invoke-static {v0, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent())

        LoadRewardedVideoFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_LOAD_RV"
            invoke-static {v0, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent())

        IsShowingBannersFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_BANNERS"
            invoke-static {v0, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent())

        IsInterstitialLoadedGroupFingerprint.method.addInstructions(0, """
            const-string v1, "HCR_IIG"
            invoke-static {v1, v1}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent())

        IsFirebaseInitializedFingerprint.method.addInstructions(0, """
            const-string v0, "HCR_FIREBASE"
            invoke-static {v0, v0}, $ALOG->d(Ljava/lang/String;Ljava/lang/String;)I
        """.trimIndent())
    }
}