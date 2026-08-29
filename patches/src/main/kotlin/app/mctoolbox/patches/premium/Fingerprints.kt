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
 * jz0.a() - PopupWindow.showAtLocation crash path 1
 * Called from: xz0.<init> → mz0.g → xs0.g → jz0.a
 */
object PopupJz0Fingerprint : Fingerprint(
    definingClass = "Ljz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * rz0.a() - PopupWindow.showAtLocation crash path 2
 * Called from: xz0.<init> → uz0.<init> → uz0.a → xs0.g → rz0.a
 */
object PopupRz0Fingerprint : Fingerprint(
    definingClass = "Lrz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * mj.a() - PopupWindow.showAtLocation crash path 3
 */
object PopupMjFingerprint : Fingerprint(
    definingClass = "Lmj;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * bz0.a() - PopupWindow.showAtLocation crash path 4
 */
object PopupBz0Fingerprint : Fingerprint(
    definingClass = "Lbz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)
