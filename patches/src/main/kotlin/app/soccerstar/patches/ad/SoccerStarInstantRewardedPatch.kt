package app.soccerstar.patches.ad

import app.morphe.patcher.patch.rawResourcePatch
import app.soccerstar.patches.shared.Constants.COMPATIBILITY_SOCCERSTAR

private val CBZ_NOP = byteArrayOf(
    0x1F, 0x20, 0x03, 0xD5.toByte(),
)

private val B_CAVE = byteArrayOf(
    0x40, 0x6E, 0xFB.toByte(), 0x17,
)

private val CAVE_STUB = byteArrayOf(
    0x60, 0x0E, 0x40, 0xF9.toByte(), 0x40, 0x00, 0x00, 0xB4.toByte(),
    0x60, 0x2C, 0x1E, 0x94.toByte(), 0xFD.toByte(), 0x7B, 0x43, 0xA9.toByte(),
    0xF4.toByte(), 0x4F, 0x42, 0xA9.toByte(), 0xF6.toByte(), 0x57, 0x41, 0xA9.toByte(),
    0xF8.toByte(), 0x5F, 0xC4.toByte(), 0xA8.toByte(), 0x20, 0x00, 0x80.toByte(), 0x52,
    0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

@Suppress("unused")
val soccerStarInstantRewarded = rawResourcePatch(
    name = "Soccer Star Instant Rewarded",
    description = "Rewarded videos grant the success callback immediately without playing an ad.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SOCCERSTAR)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        CAVE_STUB.copyInto(bytes, 0x007998B0)
        CBZ_NOP.copyInto(bytes, 0x008BDFAC)
        B_CAVE.copyInto(bytes, 0x008BDFB0)

        soFile.writeBytes(bytes)
    }
}
