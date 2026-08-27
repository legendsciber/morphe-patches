package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * ya0.smali - Premium state holder.
 * H(Z)V sets the Q:Z boolean field.
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
 */
object PremiumPopupFingerprint : Fingerprint(
    definingClass = "Ljz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * rz0.smali - Premium popup display.
 * a()V checks ya0.Q and shows PopupWindow via showAtLocation.
 */
object PremiumPopup2Fingerprint : Fingerprint(
    definingClass = "Lrz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * mj.smali - Premium popup display.
 * a()V checks ya0.Q and shows PopupWindow via showAtLocation.
 */
object PremiumPopup3Fingerprint : Fingerprint(
    definingClass = "Lmj;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)

/**
 * bz0.smali - Premium popup display.
 * run()V checks ya0.Q and shows PopupWindow via showAtLocation.
 */
object PremiumPopup4Fingerprint : Fingerprint(
    definingClass = "Lbz0;",
    name = "run",
    returnType = "V",
    parameters = listOf()
)

/**
 * t20.smali - Overlay runner with WindowManager.addView.
 * run()V adds overlay view to WindowManager.
 * Crashes in onCreate because window token is null.
 * Method start is patched to post to Handler with 1s delay.
 */
object OverlayAddViewFingerprint : Fingerprint(
    definingClass = "Lt20;",
    name = "run",
    returnType = "V",
    parameters = listOf()
)
