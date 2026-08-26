package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * Watch Ad Instant Reward — "Watch ad" butonuna basmak yeterli.
 *
 * r2$b.b() metodunun BASINA odul cagrisi enjekte edilir. Metodun ilk
 * talimati olarak tv$a.a() cagrilir ve return-void ile metot sonlanir.
 * Orijinal govde (retry mantigi, log vs.) ulasilamaz olur.
 *
 * Sonuc: Ilk reklam kaynagi basarisiz olursa bile aninda +15dk premium.
 */
@Suppress("unused")
val mctoolboxWatchAdInstantRewardPatch = bytecodePatch(
    name = "Watch Ad Instant Reward",
    description = "\"Watch ad\" butonuna basmak yeterli: reklam yukleme denemeleri beklemeden 15 dakikalik premium aninda eklenir.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        AdLoadFailFingerprint.method.addInstructions(0, """
            iget-object v0, p0, Lr2${'$'}b;->c:Ltv${'$'}a;
            invoke-interface {v0}, Ltv${'$'}a;->a()V
            return-void
        """.trimIndent())
    }
}
