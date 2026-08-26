package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * Watch Ad Instant Reward — "Watch ad" butonuna basmak yeterli.
 *
 * ── Orijinal akis ────────────────────────────────────────────────────────────
 * 1. Premium dialog "Watch ad" butonu → r2.b(tv$a, kaynak, 0)
 * 2. Reklam kaynagi sirayyla denenir (AppLovin/AdMob/...)
 * 3. Kaynak BASARISIZ → r2$b.b() → log + sonraki kaynagi dene
 * 4. Tum kaynaklar tukenir → "rewarded_ad_failed" timeout'u
 * 5. Kaynak BASARILI → c() → reklam gosterilir → izlenir → a() → +15dk
 *
 * ── Yama ─────────────────────────────────────────────────────────────────────
 * r2$b.b() govdesi degistirilir: basarisizlik aninda dogrudan tv$a.a()
 * cagrilir (odul verildi bildirimi). Ilk kaynak basarisiz olursa bile aninda
 * +15dk premium eklenir; diger kaynaklar hic denenmez, reklam asla yuklenmez.
 *
 * Not: Bir kaynak nadiren basarili yuklenirse normal akis da calisir
 * (reklam gosterilir, izlenince a() ile odul verilir) — cift odul engellenmis
 * durumda degil, Toolbox kendi stack-limit kontrolunu yapar.
 */
@Suppress("unused")
val mctoolboxWatchAdInstantRewardPatch = bytecodePatch(
    name = "Watch Ad Instant Reward",
    description = "\"Watch ad\" butonuna basmak yeterli: reklam yukleme denemeleri beklemeden 15 dakikalik premium aninda eklenir.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        val fp = AdLoadFailFingerprint
        val method = fp.method
        val matchIdx = fp.instructionMatches[0].index

        // Tum govdeyi sil (matchIdx sonrasi dahil) ve odul cagrisini yaz.
        // implementation.instructions.size() kullanilamaz cungen enjekte
        // ettigimiz blok zaten return-void ile bitiyor.
        val impl = method.implementation ?: return@bytecodePatch
        val total = impl.instructions.size()

        // matchIdx'ten itibaren tum talimatlari sil (string + geri kalan her sey)
        method.removeInstructions(matchIdx, total - matchIdx)

        // Odul bildirimini ekle: tv$a.a() = "+15 dk premium"
        method.addInstructions(matchIdx, """
            iget-object v0, p0, Lr2${'$'}b;->c:Ltv${'$'}a;
            invoke-interface {v0}, Ltv${'$'}a;->a()V
            return-void
        """.trimIndent())
    }
}
