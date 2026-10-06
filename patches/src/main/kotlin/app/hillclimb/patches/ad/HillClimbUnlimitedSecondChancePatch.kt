package app.hillclimb.patches.ad

import app.morphe.patcher.patch.rawResourcePatch
import app.hillclimb.patches.shared.Constants.COMPATIBILITY_HILLCLIMB

private val ANCHOR = byteArrayOf(
        0xE1.toByte(), 0x03, 0x18, 0x2A, 0xE2.toByte(), 0x03, 0x17, 0x2A, 0xE3.toByte(), 0x03, 0x16, 0x2A,
        0xE4.toByte(), 0x03, 0x15, 0x2A, 0xE5.toByte(), 0x03, 0x14, 0x2A, 0xE6.toByte(), 0x03, 0x13, 0x2A,
        0xF4.toByte(), 0x4F, 0x43, 0xA9.toByte(), 0xF6.toByte(), 0x57, 0x42, 0xA9.toByte(), 0xF8.toByte(), 0x5F, 0x41, 0xA9.toByte(),
        0xFD.toByte(), 0x7B, 0xC4.toByte(), 0xA8.toByte(), 0x0B, 0x76, 0x02, 0x14
)

private val RET = byteArrayOf(
        0xE1.toByte(), 0x03, 0x18, 0x2A, 0xE2.toByte(), 0x03, 0x17, 0x2A, 0xE3.toByte(), 0x03, 0x16, 0x2A,
        0xE4.toByte(), 0x03, 0x15, 0x2A, 0xE5.toByte(), 0x03, 0x14, 0x2A, 0xE6.toByte(), 0x03, 0x13, 0x2A,
        0xF4.toByte(), 0x4F, 0x43, 0xA9.toByte(), 0xF6.toByte(), 0x57, 0x42, 0xA9.toByte(), 0xF8.toByte(), 0x5F, 0x41, 0xA9.toByte(),
        0xFD.toByte(), 0x7B, 0xC4.toByte(), 0xA8.toByte(), 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte()
)

private fun indexOfUnique(bytes: ByteArray, anchor: ByteArray): Int {
    var count = 0
    var at = -1
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
        if (match) {
            count++
            at = i
            if (count > 1) break
        }
        i++
    }
    require(count == 1) { "frequency cap site not unique: $count matches" }
    return at
}

@Suppress("unused")
val hillClimbUnlimitedSecondChancePatch = rawResourcePatch(
    name = "Hill Climb Racing Unlimited Second Chance",
    description = "The rewarded interstitial frequency cap is never installed, so the rewarded second chance revive offer is no longer limited and you can revive as often as you like.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_HILLCLIMB)

    execute {
        val soFile = get("lib/arm64-v8a/libgame.so", true)
        val bytes = soFile.readBytes()

        RET.copyInto(bytes, indexOfUnique(bytes, ANCHOR))

        soFile.writeBytes(bytes)
    }
}
