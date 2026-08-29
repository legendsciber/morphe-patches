package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Phase 1: Skip xs0.g() trigger in mz0.g() during init
        Mz0TriggerFingerprint.method.addInstructions(11, "return-void")

        // Phase 2: onResume() - set Q=true on mz0.d + bridge.b.c, write SharedPrefs, post hz0
        OnResumeFingerprint.method.addInstructions(5, """
            iget-object v0, p0, Lio/mrarm/mctoolbox/MinecraftActivity;->X:Lxz0;
            iget-object v0, v0, Lxz0;->g:Lmz0;
            iget-object v0, v0, Lmz0;->d:Lya0;
            const/4 v1, 0x1
            iput-boolean v1, v0, Lya0;->Q:Z
            invoke-static {}, Lio/mrarm/mctoolbox/bridge/b;->o()Lio/mrarm/mctoolbox/bridge/b;
            move-result-object v0
            iget-object p0, v0, Lio/mrarm/mctoolbox/bridge/b;->c:Lya0;
            iput-boolean v1, p0, Lya0;->Q:Z
            const-string p0, "internal/premium_unlocked"
            invoke-virtual {v0, p0, v1}, Lio/mrarm/mctoolbox/bridge/b;->h(Ljava/lang/String;Z)V
            new-instance v1, Lhz0;
            invoke-direct {v1, v0}, Lhz0;-><init>(Lio/mrarm/mctoolbox/bridge/b;)V
            new-instance v0, Landroid/os/Handler;
            invoke-direct {v0}, Landroid/os/Handler;-><init>()V
            invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z
            return-void
        """.trimIndent())
    }
}
