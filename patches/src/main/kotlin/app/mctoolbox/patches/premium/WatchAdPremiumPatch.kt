package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * Watch Ad Instant Reward — "Watch ad" butonuna basmak yeterli.
 *
 * Premium iki anahtarla calisir:
 *   1) "internal/premium/remaining_time" (float) → sureyi S() ile yazariz
 *   2) "internal/premium_unlocked" (boolean) → R() ile true yapilir
 *
 * Sonra bridge.b.a(bridge) ile ya0.Q flag'i refresh edilir; boylece
 * ozellikler aninda acilir.
 *
 * Dort katmanli patch:
 *
 * 1. vs0.a()Z → HER ZAMAN true doner (reklam suresi dolmus gibi).
 *
 * 2. vs0.b()Z → HER ZAMAN true doner (kapatma izni verir).
 *
 * 3. r2$b.b()V → Reklam kaynagi basarisiz olursa:
 *    S() ile premium sure yazar + R() ile unlocked=true + a() ile refresh
 *    + tv$a.a() ile dialog kapatir.
 *
 * 4. n21$a.b()V → Tum kaynaklar basarisiz olursa:
 *    S() ile premium sure yazar + R() ile unlocked=true + a() ile refresh.
 *    Orijinal kod toast + dialog kapatir.
 */
@Suppress("unused")
val mctoolboxWatchAdInstantRewardPatch = bytecodePatch(
    name = "Watch Ad Instant Reward",
    description = "\"Watch ad\" butonuna basmak yeterli: reklam yukleme denemeleri beklemeden 15 dakikalik premium aninda eklenir.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. vs0.a() → HER ZAMAN true
        Vs0TimeElapsedFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 2. vs0.b() → HER ZAMAN true
        Vs0CanCloseFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 3. r2$b.b() → Basarisizlik aninda premium yaz + unlock + refresh + kapat
        // .locals 4: v0-v3 mevcut
        AdLoadFailFingerprint.method.addInstructions(0, """
            sget-object v0, Lio/mrarm/mctoolbox/bridge/b;->h:Lio/mrarm/mctoolbox/bridge/b;
            const-string v1, "internal/premium/remaining_time"
            invoke-virtual {v0, v1}, Lio/mrarm/mctoolbox/bridge/b;->n(Ljava/lang/String;)F
            move-result v2
            const/high16 v3, 0x44610000
            add-float v2, v2, v3
            invoke-virtual {v0, v1, v2}, Lio/mrarm/mctoolbox/bridge/b;->S(Ljava/lang/String;F)V
            const-string v1, "internal/premium_unlocked"
            const/4 v2, 0x1
            invoke-virtual {v0, v1, v2}, Lio/mrarm/mctoolbox/bridge/b;->R(Ljava/lang/String;Z)V
            invoke-static {v0}, Lio/mrarm/mctoolbox/bridge/b;->a(Lio/mrarm/mctoolbox/bridge/b;)V
            iget-object v0, p0, Lr2${'$'}b;->c:Ltv${'$'}a;
            invoke-interface {v0}, Ltv${'$'}a;->a()V
            return-void
        """.trimIndent())

        // 4. n21$a.b() → Tum kaynaklar basarisiz: premium yaz + unlock + refresh
        // .locals 3: v0-v2 mevcut
        AdAllSourcesFailedFingerprint.method.addInstructions(0, """
            sget-object v0, Lio/mrarm/mctoolbox/bridge/b;->h:Lio/mrarm/mctoolbox/bridge/b;
            const-string v1, "internal/premium/remaining_time"
            const/high16 v2, 0x44610000
            invoke-virtual {v0, v1, v2}, Lio/mrarm/mctoolbox/bridge/b;->S(Ljava/lang/String;F)V
            const-string v1, "internal/premium_unlocked"
            const/4 v2, 0x1
            invoke-virtual {v0, v1, v2}, Lio/mrarm/mctoolbox/bridge/b;->R(Ljava/lang/String;Z)V
            invoke-static {v0}, Lio/mrarm/mctoolbox/bridge/b;->a(Lio/mrarm/mctoolbox/bridge/b;)V
        """.trimIndent())
    }
}
