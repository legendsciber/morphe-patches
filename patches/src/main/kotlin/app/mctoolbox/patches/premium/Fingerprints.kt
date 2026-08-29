package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)

object Mz0TriggerFingerprint : Fingerprint(
    definingClass = "Lmz0;",
    name = "g",
    returnType = "V",
    parameters = listOf("Lya0;")
)

object PopupDecisionFingerprint : Fingerprint(
    definingClass = "Ltz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
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
