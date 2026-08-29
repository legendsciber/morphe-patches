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
 * t20.smali - Runnable that calls WindowManager.addView (overlay).
 * Crash: xz0.<init> → xs0.g → tz0.a → t20.run → addView → BadTokenException
 * Fix: Post self to Handler with 500ms delay, window ready by then.
 */
object OverlayShowFingerprint : Fingerprint(
    definingClass = "Lt20;",
    name = "run",
    returnType = "V",
    parameters = listOf()
)
