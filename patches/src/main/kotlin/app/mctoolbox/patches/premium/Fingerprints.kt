package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)

/**
 * xs0.g(ILandroidx/databinding/e;)V - Data binding callback.
 * We wrap it in try-catch to catch BadTokenException during init.
 */
object Xs0TriggerFingerprint : Fingerprint(
    definingClass = "Lxs0;",
    name = "g",
    returnType = "V"
)
