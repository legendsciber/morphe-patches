package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * bridge/b kurucusu — baslangicta premium durumunu okuyup UI'a baglar:
 *
 *   const-string v2, "internal/premium_unlocked"
 *   invoke-static {v2, v1}, Lio/mrarm/mctoolbox/bridge/b;->B(...)Z  (native)
 *   move-result v1
 *   invoke-virtual {v3, v1}, Lya0;->H(Z)V     <- databinding observable
 */
object PremiumStateInitFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/bridge/b;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("internal/premium_unlocked"),
        methodCall(definingClass = "Lio/mrarm/mctoolbox/bridge/b;", name = "B")
    )
)

/**
 * bridge/b.a(b) static metodu — premium durumu degistiginde UI'i gunceller:
 *
 *   const-string v1, "internal/premium_unlocked"
 *   invoke-static {v1, v0}, Lio/mrarm/mctoolbox/bridge/b;->B(...)Z  (native)
 *   move-result v0
 *   invoke-virtual {p0-c, v0}, Lya0;->H(Z)V
 *
 * Buradaki sonuc register'i v0'dir (kurucudaki v1'den farkli!).
 */
object PremiumStateNotifyFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/bridge/b;",
    name = "a",
    returnType = "V",
    parameters = listOf("Lio/mrarm/mctoolbox/bridge/b;"),
    filters = listOf(
        string("internal/premium_unlocked"),
        methodCall(definingClass = "Lio/mrarm/mctoolbox/bridge/b;", name = "B")
    )
)
