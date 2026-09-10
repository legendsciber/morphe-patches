package app.shadowfight.patches.iap

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.shadowfight.patches.shared.Constants.COMPATIBILITY_SF2

@Suppress("unused")
val sfIAPBypassSmaliPatch = bytecodePatch(
    name = "Shadow Fight 2 IAP Bypass (Smali)",
    description = "Bypasses anti-tamper security check to restore billing flow.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        IAPBypassSecurityVerifyStepFingerprint.method.addInstructionsWithLabels(0, """
            invoke-static {}, Lcom/nekki/utils/security/SecurityManager;->NotifyOnSuccess()V
            return-void
        """.trimIndent())
    }
}
