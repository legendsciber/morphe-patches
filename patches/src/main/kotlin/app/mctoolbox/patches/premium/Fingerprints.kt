package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * ya0.smali - Premium state holder.
 * <init>(Z)V constructor sets Q field from parameter.
 * We patch to always set Q=true regardless of parameter.
 * H(Z)V setter also patched to always set Q=true.
 */
object PremiumInitFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Z")
)

/**
 * ya0.smali - Premium state setter.
 * H(Z)V sets Q field and calls F() to notify observers.
 * We patch to only set Q=true, skipping F() notification.
 */
object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)
