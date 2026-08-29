package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * ya0.smali - Premium state holder.
 */
object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)

/**
 * jz0.a() - PopupWindow.showAtLocation crash path
 * Chain: xz0.<init> → mz0.g → xs0.g → jz0.a → showAtLocation
 */
object PopupJz0Fingerprint : Fingerprint(
    definingClass = "Ljz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * rz0.a() - PopupWindow.showAtLocation crash path
 * Chain: xz0.<init> → uz0.<init> → uz0.a → xs0.g → rz0.a → showAtLocation
 */
object PopupRz0Fingerprint : Fingerprint(
    definingClass = "Lrz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * mj.a() - PopupWindow.showAtLocation crash path
 */
object PopupMjFingerprint : Fingerprint(
    definingClass = "Lmj;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * tz0.a() - Runnable decision point
 * Chain: xz0.<init> → xs0.g → tz0.a → t20.run → WindowManager.addView
 * Checks ya0.Q, runs premium overlay Runnable or normal Runnable.
 */
object PopupDecisionFingerprint : Fingerprint(
    definingClass = "Ltz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)
