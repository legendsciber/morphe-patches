package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)

/**
 * mz0.g(ya0) - Registers data binding callback and immediately triggers xs0.g().
 * We patch this to SKIP the xs0.g() trigger, deferring it to onResume().
 */
object Mz0TriggerFingerprint : Fingerprint(
    definingClass = "Lmz0;",
    name = "g",
    returnType = "V",
    parameters = listOf("Lya0;")
)

/**
 * MinecraftActivity.onResume() - Called after window is ready.
 * We insert code here to:
 * 1. Set ya0.Q = true (enable premium)
 * 2. Trigger xs0.g(0, null) → tz0.a() → popup/overlay shows (window ready)
 */
object OnResumeFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/MinecraftActivity;",
    name = "onResume",
    returnType = "V",
    parameters = listOf()
)
