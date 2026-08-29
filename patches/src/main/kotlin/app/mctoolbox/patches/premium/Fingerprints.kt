package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * mz0.g(ya0) - Registers data binding callback and immediately triggers xs0.g().
 * We patch this to SKIP the xs0.g() trigger (insert return-void at index 11).
 */
object Mz0TriggerFingerprint : Fingerprint(
    definingClass = "Lmz0;",
    name = "g",
    returnType = "V",
    parameters = listOf("Lya0;")
)

/**
 * MinecraftActivity.onResume() - Called before window token is ready.
 * We insert code to:
 * 1. Write premium=true to SharedPrefs
 * 2. Post hz0 (bridge refresh) via Handler.post() → runs AFTER onResume returns
 * 3. After resume: bridge.b.a() → reads premium → ya0.H(true) → Q=true → F() → popup → success
 */
object OnResumeFingerprint : Fingerprint(
    definingClass = "Lio/mrarm/mctoolbox/MinecraftActivity;",
    name = "onResume",
    returnType = "V",
    parameters = listOf()
)
