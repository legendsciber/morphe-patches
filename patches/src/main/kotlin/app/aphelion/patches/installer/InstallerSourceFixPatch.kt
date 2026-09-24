package app.aphelion.patches.installer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.aphelion.patches.shared.Constants.COMPATIBILITY_APHELION
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val INSTALLER_PACKAGE_NAME = "com.android.vending"

private fun Fingerprint.spoofInstallerSource() {
    val moveResultIndex = instructionMatches[0].index + 1
    val register = (method.getInstruction(moveResultIndex) as OneRegisterInstruction).registerA
    method.replaceInstruction(moveResultIndex, "const-string v$register, \"$INSTALLER_PACKAGE_NAME\"")
}

@Suppress("unused")
val aphelionInstallerSourceFix = bytecodePatch(
    name = "Aphelion Installer Source Fix",
    description = "Spoofs the installer source as Google Play for every install-source check the game performs, so sideloaded installs pass and the forced Google Play Store redirect on launch is fixed.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_APHELION)

    execute {
        AdSdkInstallerFingerprint.spoofInstallerSource()
        LicenseInstallerFingerprint.spoofInstallerSource()
        Wn1InstallerFingerprint.spoofInstallerSource()
        Pd2InstallerFingerprint.spoofInstallerSource()
        Ny4InstallerFingerprint.spoofInstallerSource()
        Wu4InstallingInstallerFingerprint.spoofInstallerSource()
        Wu4InitiatingInstallerFingerprint.spoofInstallerSource()
    }
}
