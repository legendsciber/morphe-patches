package app.mctoolbox.patches.premium

import app.morphe.patcher.Fingerprint

/**
 * ya0.smali - Premium state holder.
 * H(Z)V sets the Q:Z boolean field and notifies observers.
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
 * Crashes in onCreate because window token is null.
 * Body is replaced: return immediately (skip popup).
 */
object PremiumPopupFingerprint : Fingerprint(
    definingClass = "Ljz0;",
    name = "a",
    returnType = "V",
    parameters = listOf()
)
