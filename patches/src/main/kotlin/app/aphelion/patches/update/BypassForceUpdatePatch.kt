package app.aphelion.patches.update

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.aphelion.patches.shared.Constants.COMPATIBILITY_APHELION

@Suppress("unused")
val aphelionBypassForceUpdate = bytecodePatch(
    name = "Aphelion Force Update Bypass",
    description = "Stops the forced Google Play Store update redirect on launch: the in-app update check always passes, so the game opens normally even when the remote minimum version requirement exceeds the installed version.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_APHELION)

    execute {
        ForceUpdateVersionCheckFingerprint.method.replaceInstruction(
            ForceUpdateVersionCheckFingerprint.instructionMatches[2].index,
            "const-wide/16 v0, 0x7fff",
        )
    }
}
