package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * xs0.smali - Data binding callback.
 * g(ILandroidx/databinding/e;)V calls xs0$a.a() which triggers popups.
 * We make this a no-op to prevent all popup crash chains.
 */
object DataBindingCallbackFingerprint : Fingerprint(
    definingClass = "Lxs0;",
    name = "g",
    returnType = "V",
    parameters = listOf("I", "Landroidx/databinding/e;")
)

/**
 * ya0.smali - Premium state holder.
 * H(Z)V setter sets Q and calls F().
 */
object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)
