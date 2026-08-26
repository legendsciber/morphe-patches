package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * Watch Ad Instant Reward — "Watch ad" butonuna basmak yeterli.
 *
 * Üç katmanlı patch:
 *
 * 1. vs0.a()Z → HER ZAMAN true döner (reklam süresi dolmuş gibi davranır).
 *    Reklam ekranı açıldığında süre kontrolü hemen geçer, ekran kapanır.
 *
 * 2. vs0.b()Z → HER ZAMAN true döner (kapatma izni verir).
 *    Erken kapatma diyalogu ve ödül koşulu her zaman sağlanır.
 *
 * 3. r2$b.b()V → Reklam yüklenemezse bile direkt ödül verilir.
 *    tv$a.d() → bridge.b.t() → native I(900.0f) ile +15dk premium ekler,
 *    tv$a.a() → dialog kapatır ve n21.W flag'ini sıfırlar.
 *
 * Normal akış: reklam yüklenir → vs0.a() true → ekran kapanır →
 * z2.a() → tv$a.d() → bridge.b.t() → +15dk premium.
 *
 * Hata akışı: reklam yüklenemez → r2$b.b() → tv$a.d() → +15dk premium.
 */
@Suppress("unused")
val mctoolboxWatchAdInstantRewardPatch = bytecodePatch(
    name = "Watch Ad Instant Reward",
    description = "\"Watch ad\" butonuna basmak yeterli: reklam yükleme denemeleri beklemeden 15 dakikalık premium anında eklenir.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1. vs0.a() → HER ZAMAN true (reklam süresi dolmuş gibi davranır)
        Vs0TimeElapsedFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 2. vs0.b() → HER ZAMAN true (kapatma izni verir)
        Vs0CanCloseFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """.trimIndent())

        // 3. r2$b.b() → Reklam yüklenemezse bile ödül ver
        AdLoadFailFingerprint.method.addInstructions(0, """
            iget-object v0, p0, Lr2${'$'}b;->c:Ltv${'$'}a;
            invoke-interface {v0}, Ltv${'$'}a;->d()V
            invoke-interface {v0}, Ltv${'$'}a;->a()V
            return-void
        """.trimIndent())
    }
}
