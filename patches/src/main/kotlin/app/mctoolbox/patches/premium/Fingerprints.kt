package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * Lvs0.a()Z — "reklam suresi doldu mu?" kontrolu.
 * currentTimeMillis >= baslangic + sure*1000 karsilastirmasi yapar.
 *
 * Govdesi tamamen degistirilir: HER ZAMAN true doner.
 */
object Vs0TimeElapsedFingerprint : Fingerprint(
    definingClass = "Lvs0;",
    name = "a",
    returnType = "Z",
    parameters = listOf()
)

/**
 * Lvs0.b()Z — ikinci tamamlandi kontrolu (erken kapatma diyalogu ve
 * odul kosulu icin kullanilan diger boolean).
 *
 * Govdesi tamamen degistirilir: HER ZAMAN true doner.
 */
object Vs0CanCloseFingerprint : Fingerprint(
    definingClass = "Lvs0;",
    name = "b",
    returnType = "Z",
    parameters = listOf()
)

/**
 * SimpleInterstitialAdActivity.r() — reklam ekraninin geri sayim tick'i.
 * Butona basilinca aktivite olusturulur ve bu metod 100ms sonra ilk kez
 * calisir. Govde basina finish() enjekte edilir: aktivite hic icerik
 * gostermadan aninda kapanir ve finish() icindeki odul yolu isler.
 */
object AdScreenTickFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/simpleads/SimpleInterstitialAdActivity;",
    name = "r",
    returnType = "V",
    parameters = listOf()
)

/**
 * Ln21$a.b()V — tum reklam kaynaklari basarisiz oldugunda cagrilir.
 * Toast gosterir, dialog'u kapatir ve n21.W flag'ini sifirlar.
 *
 * Basarisizlik aninda premium sure dogrudan bridge.b.S() ile yazilir.
 */
object AdAllSourcesFailedFingerprint : Fingerprint(
    definingClass = "Ln21\$a;",
    name = "b",
    returnType = "V",
    parameters = listOf()
)

/**
 * Lm21.onClick(Landroid/view/View;)V — "Watch ad" butonuna tiklandiginda
 * calisir. premium_ticket string'ini kullanarak r2.b() cagrisini yapar.
 *
 * Butona tiklaninca reklam yukleme tamamen atlanir, S() ile dogrudan
 * premium sure yazilir ve dialog kapatilir.
 */
object M21OnClickFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    strings = listOf("premium_ticket")
)
