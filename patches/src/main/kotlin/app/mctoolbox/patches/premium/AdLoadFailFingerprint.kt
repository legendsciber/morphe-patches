package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * Lr2$b.b()V — reklam kaynagi yukleme BASARISIZLIGI handler'i.
 *
 * "Failed to load ad from <source>" loglar ve SONRAKI kaynagi dener.
 * Tum kaynaklar tukenince kullaniciya "rewarded_ad_failed" timeout'u doner.
 *
 * Govdesi degistirilir: basarisizlik aninda dogrudan odul verilir.
 */
object AdLoadFailFingerprint : Fingerprint(
    definingClass = "Lr2$b;",
    name = "b",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("Failed to load ad from ")
    )
)
