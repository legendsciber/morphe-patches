package app.soccerstar.patches.ad

import app.morphe.patcher.patch.rawResourcePatch
import app.soccerstar.patches.shared.Constants.COMPATIBILITY_SOCCERSTAR

private val ENABLE_FALSE = byteArrayOf(
    0x00, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val RET = byteArrayOf(
    0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val INTERSTITIAL_OFFSETS = intArrayOf(
    0x008BEBBC,
    0x00C5247C,
)

private val BANNER_OFFSETS = intArrayOf(
    0x008BE1A0,
    0x00C51178,
)

@Suppress("unused")
val soccerStarAdRemoval = rawResourcePatch(
    name = "Soccer Star Ad Removal",
    description = "Disables ads completely: EnableAD always returns false, interstitials and banners are no-ops, Adjust purchase verification is skipped.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SOCCERSTAR)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        ENABLE_FALSE.copyInto(bytes, 0x008BD77C)

        for (offset in INTERSTITIAL_OFFSETS) {
            ENABLE_FALSE.copyInto(bytes, offset)
        }

        for (offset in BANNER_OFFSETS) {
            RET.copyInto(bytes, offset)
        }

        RET.copyInto(bytes, 0x008B37E8)

        soFile.writeBytes(bytes)
    }
}
