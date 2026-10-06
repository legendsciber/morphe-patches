package app.hillclimb.patches.ad

import app.morphe.patcher.patch.rawResourcePatch
import app.hillclimb.patches.shared.Constants.COMPATIBILITY_HILLCLIMB

private val GATE_ANCHOR = byteArrayOf(
    0x14, 0x06, 0x00, 0x37, 0xE0.toByte(), 0x05, 0x00, 0x37
)

private val GATE_REPLACEMENT = byteArrayOf(
    0xD5.toByte(), 0x03, 0x20, 0x1F, 0xD5.toByte(), 0x03, 0x20, 0x1F
)

private val FLAG_ANCHOR = byteArrayOf(
    0x60, 0xC2.toByte(), 0x41, 0xF9.toByte(), 0x75, 0xE2.toByte(), 0x46, 0x39, 0x80.toByte(), 0x00, 0x00, 0xB4.toByte(),
    0x08, 0x00, 0x40, 0xF9.toByte(), 0x08, 0x61, 0x40, 0xF9.toByte()
)

private val FLAG_REPLACEMENT = byteArrayOf(
    0x60, 0xC2.toByte(), 0x41, 0xF9.toByte(), 0x15, 0x00, 0x80.toByte(), 0x52, 0x80.toByte(), 0x00, 0x00, 0xB4.toByte(),
    0x08, 0x00, 0x40, 0xF9.toByte(), 0x08, 0x61, 0x40, 0xF9.toByte()
)

private fun indexOfAll(bytes: ByteArray, anchor: ByteArray, expected: Int): List<Int> {
    val found = mutableListOf<Int>()
    var i = 0
    while (i <= bytes.size - anchor.size) {
        var match = true
        var j = 0
        while (j < anchor.size) {
            if (bytes[i + j] != anchor[j]) {
                match = false
                break
            }
            j++
        }
        if (match) found.add(i)
        i++
    }
    require(found.size == expected) { "site count " + found.size + ", expected " + expected }
    return found
}

@Suppress("unused")
val hillClimbUnlimitedSecondChancePatch = rawResourcePatch(
    name = "Hill Climb Racing Unlimited Second Chance",
    description = "The engine always evaluates the watched-to-continue revive offer and the per-run used flag can no longer suppress it, so you can revive after every crash instead of only once.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_HILLCLIMB)

    execute {
        val soFile = get("lib/arm64-v8a/libgame.so", true)
        val bytes = soFile.readBytes()

        for (at in indexOfAll(bytes, GATE_ANCHOR, 1)) {
            GATE_REPLACEMENT.copyInto(bytes, at)
        }

        for (at in indexOfAll(bytes, FLAG_ANCHOR, 2)) {
            FLAG_REPLACEMENT.copyInto(bytes, at)
        }

        soFile.writeBytes(bytes)
    }
}
