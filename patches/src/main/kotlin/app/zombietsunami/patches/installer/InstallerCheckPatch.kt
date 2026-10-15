package app.zombietsunami.patches.installer

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.zombietsunami.patches.shared.Constants.COMPATIBILITY_ZOMBIETSUNAMI
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val GOOGLE_PLAY = "com.android.vending"

@Suppress("unused")
val zombieTsunamiInstallerCheckPatch = bytecodePatch(
    name = "Zombie Tsunami Installer Check Fix",
    description = "The game's PairIP license client asks Android who installed the package and treats anything other than the Play Store as a failed check, so sideloaded installs are rejected on launch. This makes that one check report the Play Store as the installer.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ZOMBIETSUNAMI)

    execute {
        val fingerprint = LocalInstallerCheckFingerprint
        val moveResultIndex = fingerprint.instructionMatches[0].index + 1
        val register = (fingerprint.method.getInstruction(moveResultIndex) as OneRegisterInstruction).registerA
        fingerprint.method.replaceInstruction(
            moveResultIndex,
            "const-string v$register, \"$GOOGLE_PLAY\"",
        )
    }
}
