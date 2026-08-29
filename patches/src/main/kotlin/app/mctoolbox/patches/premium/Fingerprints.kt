package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * xs0.g() - Data binding callback that triggers popup/overlay chain.
 * Non-synthetic class, so addInstructionsWithLabels should work.
 * We wrap the invoke-interface in try-catch to catch BadTokenException.
 */
object Xs0TriggerFingerprint : Fingerprint(
    definingClass = "Lxs0;",
    name = "g",
    returnType = "V",
    parameters = listOf("ILandroidx/databinding/e;")
)
