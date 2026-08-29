package app.mctoolbox.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.mctoolbox.patches.shared.Constants.COMPATIBILITY_MCTOOLBOX

/**
 * MCToolbox Premium (Under Testing)
 *
 * 1. ya0.H() → always set Q=true, skip F() → premium active, no data binding trigger during init
 * 2. mz0.g() → skip xs0.g() trigger → prevents BadTokenException during init
 *
 * Result: App opens, premium active, overlay shows through app's own mechanism.
 */
@Suppress("unused")
val mctoolboxPremiumPatch = bytecodePatch(
    name = "Premium (Under Testing)",
    description = "Enables premium directly without watching ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MCTOOLBOX)

    execute {
        // Enable premium: Q=true, skip F() (no data binding trigger during init)
        SetPremiumStateFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            iput-boolean v0, p0, Lya0;->Q:Z
            return-void
        """.trimIndent())

        // Prevent crash: skip xs0.g() trigger in mz0.g() during init
        Mz0TriggerFingerprint.method.addInstructions(11, "return-void")
    }
}
