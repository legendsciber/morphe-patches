package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * Instant Watch Ad Premium — "Watch ad" akisini reklamsiz ve anlik yapar.
 *
 * ── Mekanizma ────────────────────────────────────────────────────────────────
 * Toolbox premium durumunu native tarafta tutar (JNI: bridge/b.B(key, def)Z).
 * "internal/premium_unlocked" anahtari icin donen deger, databinding
 * observable'ina (Lya0;->H(Z)) verilerek Toolbox menüsündeki premium
 * özelliklerini açar/kapar.
 *
 * Native B() metodu baska anahtarlar icin de kullanilan genel bir getter
 * oldugu ICIN dokunulmaz. Bunun yerine yalnizca iki cagri noktasinda okuma
 * sonucu 1'e zorlanir:
 *
 *   1) bridge/b.<init>()   → baslangicta premium=acik bildirilir
 *   2) bridge/b.a(b)       → durum guncellemelerinde de acik kalir
 *
 * Sonuc: hicbir reklam goruntulenmeden Toolbox ozellikleri surekli acik.
 * ("15 dakika" suresi anlamini yitirir — surekli premium.)
 *
 * ── Enjeksiyon guvenligi ────────────────────────────────────────────────────
 * Her iki sitede desen: invoke(B) [+0] / move-result vR [+1] / H(Z) cagrisi.
 * const/4 vR,0x1 +1 konumunda move-result'i takip ettigi icin invoke-move-result
 * baglantisi korunur ve int->int tip akisi verifier'dan temiz gecer.
 */
@Suppress("unused")
val mctoolboxWatchAdPremiumPatch = bytecodePatch(
    name = "Instant Watch Ad Premium",
    description = "\"Watch ad\" butonuna basmak yeterli: reklam izlemeden premium ozellikler aninda acilir ve acik kalir.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // 1) Kurucu: sonuc register'i v1
        val initFp = PremiumStateInitFingerprint
        initFp.method.addInstructions(
            initFp.instructionMatches[1].index + 2,
            """
                const/4 v1, 0x1
            """.trimIndent()
        )

        // 2) Bildirim metodu (a): sonuc register'i v0
        val notifyFp = PremiumStateNotifyFingerprint
        notifyFp.method.addInstructions(
            notifyFp.instructionMatches[1].index + 2,
            """
                const/4 v0, 0x1
            """.trimIndent()
        )
    }
}
