package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * jz0.smali - Data binding callback that shows premium popup.
 * Checks ya0.Q, if true → PopupWindow.showAtLocation → crashes during init.
 * We wrap showAtLocation in try-catch for BadTokenException.
 */
object PremiumPopupFingerprint : Fingerprint(
    definingClass = "Ljz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * ya0.smali - Premium state holder.
 * H(Z)V setter sets Q and calls F().
 */
object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)
