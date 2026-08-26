package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * Watch Ad Instant Reward — "Watch ad" butonuna basmak yeterli.
 *
 * Native I(F)V methodu reklam izlenmeden calismaz (adi SDK dogrulamasi gerekir).
 * Bunun yerine bridge.b.S(key, float) ile dogrudan SharedPreferences'a yazariz.
 *
 * dort katmanli patch:
 *
 * 1. vs0.a()Z → HER ZAMAN true doner (reklam suresi dolmus gibi).
 *    Reklam ekrani acildiginda suresi hemen dolar, ekrani kapatir.
 *
 * 2. vs0.b()Z → HER ZAMAN true doner (kapatma izni verir).
 *    Erken kapatma diyalogu ve odul kosulu her zaman saglanir.
 *
 * 3. r2$b.b()V → Reklam kaynagi basarisiz olursa S() ile premium yazar + kapatir.
 *
 * 4. n21$a.b()V → Tum kaynaklar basarisiz olursa S() ile premium yazar.
 *
 * Normal akis: reklam yuklenir → vs0.a() true → ekran kapanir →
 * finish() → vs0.h → tv$a.d() → bridge.b.t() → +15dk premium.
 *
 * Hata akisi: reklam yuklenemez → r2$b.b() veya n21$a.b() →
 * S("internal/premium/remaining_time", 900.0f) → +15dk premium.
 */
@Suppress("unused")
val mctoolboxWatchAdInstantRewardPatch = bytecodePatch(
    name = "Watch Ad Instant Reward",
    description = "\"Watch ad\" butonuna basmak yeterli: reklam yukleme denemeleri beklemeden 15 dakikalik premium aninda eklenir.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. vs0.a() → HER ZAMAN true (reklam suresi dolmus gibi davranir)
        Vs0TimeElapsedFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 2. vs0.b() → HER ZAMAN true (kapatma izni verir)
        Vs0CanCloseFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 3. r2$b.b() → Basarisizlik aninda S() ile premium yaz + kapat
        // .locals 4: v0-v3 mevcut
        AdLoadFailFingerprint.method.addInstructions(0, """
            sget-object v0, Lio/mrarm/mctoolbox/bridge/b;->h:Lio/mrarm/mctoolbox/bridge/b;
            const-string v1, "internal/premium/remaining_time"
            invoke-virtual {v0, v1}, Lio/mrarm/mctoolbox/bridge/b;->n(Ljava/lang/String;)F
            move-result v2
            const/high16 v3, 0x44610000
            add-float v2, v2, v3
            invoke-virtual {v0, v1, v2}, Lio/mrarm/mctoolbox/bridge/b;->S(Ljava/lang/String;F)V
            iget-object v0, p0, Lr2${'$'}b;->c:Ltv${'$'}a;
            invoke-interface {v0}, Ltv${'$'}a;->a()V
            return-void
        """.trimIndent())

        // 4. n21$a.b() → Tum kaynaklar basarisiz olursa S() ile premium yaz
        // .locals 3: v0-v2 mevcut
        AdAllSourcesFailedFingerprint.method.addInstructions(0, """
            sget-object v0, Lio/mrarm/mctoolbox/bridge/b;->h:Lio/mrarm/mctoolbox/bridge/b;
            const-string v1, "internal/premium/remaining_time"
            const/high16 v2, 0x44610000
            invoke-virtual {v0, v1, v2}, Lio/mrarm/mctoolbox/bridge/b;->S(Ljava/lang/String;F)V
        """.trimIndent())
    }
}
