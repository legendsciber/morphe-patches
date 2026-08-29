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

object OnResumeFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/MinecraftActivity;",
    name = "onResume",
    returnType = "V",
    parameters = listOf()
)
