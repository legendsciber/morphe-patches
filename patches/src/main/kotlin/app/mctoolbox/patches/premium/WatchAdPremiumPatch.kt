package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * Two-phase approach:
 *
 * Phase 1 - During onCreate (init):
 *   mz0.g(ya0): skip xs0.g() trigger → no crash during init
 *   (F() fires during ya0 construction BEFORE mz0.g() registers callback, so no trigger)
 *
 * Phase 2 - After onResume (window ready):
 *   1. Write premium=true to SharedPrefs
 *   2. Post hz0 (bridge.b.a() refresh) via Handler.post()
 *   3. After resume: bridge.b.a() reads SharedPrefs → ya0.H(true) → Q=true → F()
 *      → triggers registered callbacks → popup/overlay → window ready → success
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Phase 1: mz0.g(ya0) - defer the xs0.g() trigger
        // Insert return-void before the invoke-virtual to xs0.g(ILandroidx/databinding/e;)V
        // This skips the callback trigger during init
        Mz0TriggerFingerprint.method.addInstructions(11, "return-void")

        // Phase 2: onResume() - write premium to SharedPrefs + post bridge refresh
        // Insert before return-void (index 5):
        // 1. Write "internal/premium_unlocked" = true to SharedPreferences
        // 2. Create Handler + hz0 (bridge refresh Runnable) + Handler.post()
        // This defers the bridge refresh to AFTER onResume returns (window ready)
        OnResumeFingerprint.method.addInstructions(5, """
            invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
            move-result-object v0
            invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
            move-result-object v0
            const-string v1, "internal/premium_unlocked"
            const/4 p0, 0x1
            invoke-interface {v0, v1, p0}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
            move-result-object v0
            invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
            new-instance v0, Landroid/os/Handler;
            invoke-direct {v0}, Landroid/os/Handler;-><init>()V
            invoke-static {}, Lio/mrarm/mctoolbox/bridge/b;->o()Lio/mrarm/mctoolbox/bridge/b;
            move-result-object p0
            new-instance v1, Lhz0;
            invoke-direct {v1, p0}, Lhz0;-><init>(Lio/mrarm/mctoolbox/bridge/b;)V
            invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z
        """.trimIndent())
    }
}
