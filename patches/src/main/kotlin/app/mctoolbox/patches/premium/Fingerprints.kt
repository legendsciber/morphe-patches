package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * ya0.smali - Premium state holder.
 * <init>(Z)V constructor sets Q from parameter.
 * H(Z)V setter sets Q and calls F() which triggers data binding.
 * Data binding callbacks (xs0.g -> jz0.a) show popups that crash.
 * Both patched to set Q=true WITHOUT calling F().
 */
object PremiumInitFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Z")
)

object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)
