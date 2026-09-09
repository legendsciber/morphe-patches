package app.shadowfight.patches.iap

import app.morphe.patcher.patch.bytecodePatch
import app.shadowfight.patches.shared.Constants.COMPATIBILITY_SF2

/**
 * Shadow Fight 2 IAP Bypass — DISABLED (v8)
 *
 * All interceptions removed for testing.
 * If the connection error persists with this version, the issue
 * is NOT caused by our patches.
 */
@Suppress("unused")
val sfIAPBypassSmaliPatch = bytecodePatch(
    name = "Shadow Fight 2 IAP Bypass (Smali)",
    description = "IAP bypass - currently disabled for testing.",
    default = false
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        // No interceptions - pure test
    }
}
