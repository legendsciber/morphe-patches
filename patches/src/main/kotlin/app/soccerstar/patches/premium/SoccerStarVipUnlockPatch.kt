package app.soccerstar.patches.premium

import app.morphe.patcher.patch.rawResourcePatch
import app.soccerstar.patches.shared.Constants.COMPATIBILITY_SOCCERSTAR

private val GETINT_CAVE = byteArrayOf(
    0x7F, 0x22, 0x00, 0x71, 0xC0.toByte(), 0x00, 0x00, 0x54,
    0x7F, 0x26, 0x00, 0x71, 0x80.toByte(), 0x00, 0x00, 0x54,
    0x48, 0x00, 0x00, 0x37, 0xEC.toByte(), 0x32, 0x00, 0x14,
    0x17, 0x33, 0x00, 0x14, 0xFD.toByte(), 0x7B, 0x42, 0xA9.toByte(),
    0xF4.toByte(), 0x4F, 0x41, 0xA9.toByte(), 0xF5.toByte(), 0x07, 0x43, 0xF8.toByte(),
    0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val GETINT_PATCH = byteArrayOf(
    0x10, 0xCD.toByte(), 0xFF.toByte(), 0x17,
)

private val UNSUB_RET = byteArrayOf(
    0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val CHECKSUB_NOP = byteArrayOf(
    0x1F, 0x20, 0x03, 0xD5.toByte(),
)

@Suppress("unused")
val soccerStarVipUnlock = rawResourcePatch(
    name = "Soccer Star VIP Unlock",
    description = "Unlocks VIP subscription permanently: ownership keys always report active and unsubscribe can never clear the flag.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SOCCERSTAR)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        GETINT_CAVE.copyInto(bytes, 0x00D874F0)
        GETINT_PATCH.copyInto(bytes, 0x00D940B0)
        UNSUB_RET.copyInto(bytes, 0x0171C620)
        CHECKSUB_NOP.copyInto(bytes, 0x008A5D60)

        soFile.writeBytes(bytes)
    }
}
