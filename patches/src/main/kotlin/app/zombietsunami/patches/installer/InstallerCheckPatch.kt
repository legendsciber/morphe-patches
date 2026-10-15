package app.zombietsunami.patches.installer

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.zombietsunami.patches.shared.Constants.COMPATIBILITY_ZOMBIETSUNAMI
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val GOOGLE_PLAY = "com.android.vending"

/**
 * PairIP's license client is the first thing that runs in this game, and its local check asks
 * Android who installed the package. Android only answers "com.android.vending" for installs that
 * really came through the Play Store, so a sideloaded copy fails the comparison and never reaches
 * the state that lets the rest of the flow proceed.
 *
 * The patch answers that one read with the Play Store package name, which is the smallest change
 * that gets the check to pass: everything downstream - the null handling, the comparison itself,
 * the flags checked before it - stays exactly as shipped. It mirrors the generic
 * `changePackageInstallerPatch`, but scoped to the single call site that decides anything here
 * rather than rewriting every attribution SDK's copy of the same query.
 */
@Suppress("unused")
val zombieTsunamiInstallerCheckPatch = bytecodePatch(
    name = "Zombie Tsunami Installer Check Fix",
    description = "The game's PairIP license client asks Android who installed the package and treats anything other than the Play Store as a failed check, so sideloaded installs are rejected on launch. This makes that one check report the Play Store as the installer.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ZOMBIETSUNAMI)

    execute {
        val fingerprint = LocalInstallerCheckFingerprint
        // The call sits directly ahead of its move-result, so overwriting the result register
        // leaves the surrounding control flow untouched.
        val moveResultIndex = fingerprint.instructionMatches[0].index + 1
        val register = (fingerprint.method.getInstruction(moveResultIndex) as OneRegisterInstruction).registerA
        fingerprint.method.replaceInstruction(
            moveResultIndex,
            "const-string v$register, \"$GOOGLE_PLAY\"",
        )
    }
}
