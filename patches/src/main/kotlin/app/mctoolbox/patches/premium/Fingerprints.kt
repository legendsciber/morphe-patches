package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)

object PopupJz0Fingerprint : Fingerprint(
    definingClass = "Ljz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

object PopupRz0Fingerprint : Fingerprint(
    definingClass = "Lrz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

object PopupMjFingerprint : Fingerprint(
    definingClass = "Lmj;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * tz0.a() - ALL overlay/popup Runnables go through here.
 * Checks Q and runs different Runnables (t20, s20, etc.).
 * ALL of them crash during init (BadTokenException).
 * Making this a no-op prevents ALL crashes.
 */
object PopupDecisionFingerprint : Fingerprint(
    definingClass = "Ltz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)
