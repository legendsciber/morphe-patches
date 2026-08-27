package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * ya0.smali - Premium state holder.
 * H(Z)V sets the Q:Z boolean field.
 * Body is replaced: always set Q=true without notifying observers.
 */
object SetPremiumStateFingerprint : Fingerprint(
    definingClass = "Lya0;",
    name = "H",
    returnType = "V",
    parameters = listOf("Z")
)

/**
 * jz0.smali - Premium popup display.
 * a()V checks ya0.Q and shows PopupWindow via showAtLocation.
 * Body is replaced: return immediately (skip popup).
 */
object PremiumPopupFingerprint : Fingerprint(
    definingClass = "Ljz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * rz0.smali - Premium popup display (second popup).
 * a()V checks ya0.Q and shows PopupWindow via showAtLocation.
 * Body is replaced: return immediately (skip popup).
 */
object PremiumPopup2Fingerprint : Fingerprint(
    definingClass = "Lrz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * mj.smali - Premium popup display (third popup).
 * a()V checks ya0.Q and shows PopupWindow via showAtLocation.
 * Body is replaced: return immediately (skip popup).
 */
object PremiumPopup3Fingerprint : Fingerprint(
    definingClass = "Lmj;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * bz0.smali - Premium popup display (fourth popup).
 * run()V checks ya0.Q and shows PopupWindow via showAtLocation.
 * Body is replaced: return immediately (skip popup).
 */
object PremiumPopup4Fingerprint : Fingerprint(
    definingClass = "Lbz0;",
    name = "run",
    returnType = "V",
    parameters = listOf()
)
