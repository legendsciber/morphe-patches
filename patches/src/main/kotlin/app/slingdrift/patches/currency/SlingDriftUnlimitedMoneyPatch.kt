package app.slingdrift.patches.currency

import app.morphe.patcher.patch.rawResourcePatch
import app.slingdrift.patches.shared.Constants.COMPATIBILITY_SLINGDRIFT

private val FORCE_TRUE = byteArrayOf(
    0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val SITES = listOf(
    "MarketBuyButton.CanAfford" to byteArrayOf(
        0x00, 0xE0.toByte(), 0x41, 0x39, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
        0xE1.toByte(), 0x03, 0x1F, 0xAA.toByte(),
    ),
)

private fun indexOfUnique(bytes: ByteArray, anchor: ByteArray, label: String): Int {
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
    require(count == 1) { "$label site not unique: $count matches" }
    return at
}

@Suppress("unused")
val slingDriftUnlimitedMoneyPatch = rawResourcePatch(
    name = "Sling Drift Unlimited Money",
    description = "Every car in the market is always affordable, so rubies never run out and any car can be bought without saving up first.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SLINGDRIFT)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        for ((label, anchor) in SITES) {
            FORCE_TRUE.copyInto(bytes, indexOfUnique(bytes, anchor, label))
        }

        soFile.writeBytes(bytes)
    }
}
