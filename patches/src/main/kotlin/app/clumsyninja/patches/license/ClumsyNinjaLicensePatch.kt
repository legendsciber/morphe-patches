package app.clumsyninja.patches.license

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.clumsyninja.patches.shared.Constants.COMPATIBILITY_CLUMSYNINJA

private const val CALLBACK_CLASS = "Lorg/naturalmotion/NmgSystem/NmgMarketplaceGooglePlayApkExpansion\$1;"

@Suppress("unused")
val clumsyNinjaLicensePatch = bytecodePatch(
    name = "Clumsy Ninja License Bypass",
    description = "Bypasses Google Play license and source verification at startup.",
    default = true
) {
    compatibleWith(COMPATIBILITY_CLUMSYNINJA)

    execute {
        // Make installer always report Google Play (3)
        GetApplicationInstallerFingerprint.method.addInstructions(0, """
            const/4 v0, 0x3
            return v0
        """.trimIndent())

        // Redirect license failure to success
        DontAllowFingerprint.method.addInstructions(0, """
            invoke-virtual {p0, p1}, $CALLBACK_CLASS->allow(I)V
            return-void
        """.trimIndent())

        ApplicationErrorFingerprint.method.addInstructions(0, """
            invoke-virtual {p0, p1}, $CALLBACK_CLASS->allow(I)V
            return-void
        """.trimIndent())
    }
}
