package app.clumsyninja.patches.permission

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.clumsyninja.patches.shared.Constants.COMPATIBILITY_CLUMSYNINJA

private const val CALLBACK_CLASS = "Lorg/naturalmotion/NmgSystem/NmgMarketplaceGooglePlayApkExpansion\$1;"

@Suppress("unused")
val clumsyNinjaLicensePatch = bytecodePatch(
    name = "License Bypass",
    description = "Redirects Google Play license failure callbacks to allow.",
    default = true
) {
    compatibleWith(COMPATIBILITY_CLUMSYNINJA)

    execute {
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
