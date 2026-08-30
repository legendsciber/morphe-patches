package app.pcr.patches.iap

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.pcr.patches.shared.Constants.COMPATIBILITY_PCR

@Suppress("unused")
val pcrIAPBypassPatch = bytecodePatch(
    name = "IAP Bypass",
    description = "Bypasses in-app purchase verification. All purchases are treated as completed.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PCR)

    execute {
        PurchaseStateFingerprint.method.addInstructions(0, """
            const-wide v0, 0x40c9648000000000L
            return-wide v0
        """.trimIndent())

        VerifySignatureFingerprint.method.addInstructions(0, """
            const-wide/high16 v0, 0x3ff0000000000000L
            return-wide v0
        """.trimIndent())
    }
}
