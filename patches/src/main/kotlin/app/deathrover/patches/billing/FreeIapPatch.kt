package app.deathrover.patches.billing

import app.morphe.patcher.patch.rawResourcePatch
import app.deathrover.patches.shared.Constants.COMPATIBILITY_DEATHROVER

private val RET = byteArrayOf(0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte())

private class Site(
    val label: String,
    val anchor: ByteArray,
    val replacement: ByteArray,
    val requireRetPadding: Boolean,
)

private val SITES = listOf(
    Site(
        "IAPController.IsProductPurchased",
        byteArrayOf(0x60, 0x00, 0x1F, 0xD6.toByte(), 0x9E.toByte(), 0x18, 0xFB.toByte(), 0x97.toByte()),
        byteArrayOf(0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte()),
        false,
    ),
    Site(
        "ProgressManager.IsUpgradePurchased",
        byteArrayOf(0xFE.toByte(), 0x0F, 0x1F, 0xF8.toByte(), 0x21, 0xDA.toByte(), 0xFA.toByte(), 0x97.toByte()),
        byteArrayOf(0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte()),
        false,
    ),
    Site(
        "IAPController.BuyItem",
        byteArrayOf(
            0xE0.toByte(), 0x03, 0x13, 0xAA.toByte(), 0xF4.toByte(), 0x4F, 0x42, 0xA9.toByte(),
            0xF6.toByte(), 0x57, 0x41, 0xA9.toByte(), 0xFE.toByte(), 0x5F, 0xC3.toByte(), 0xA8.toByte(),
            0x05, 0x00, 0x00, 0x14, 0xF4.toByte(), 0x4F, 0x42, 0xA9.toByte(),
            0xF6.toByte(), 0x57, 0x41, 0xA9.toByte(), 0xFE.toByte(), 0x5F, 0xC3.toByte(), 0xA8.toByte(),
            0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
        ),
        byteArrayOf(0xC9.toByte(), 0x5B, 0x0A, 0x14),
        false,
    ),
    Site(
        "IAPController.BuyItem.freeGrantCave",
        byteArrayOf(
            0xE0.toByte(), 0x17, 0x9F.toByte(), 0x1A, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
            0x1F, 0x00, 0x01, 0xEB.toByte(), 0xE0.toByte(), 0x17, 0x9F.toByte(), 0x1A,
            0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(), 0x1F, 0x00, 0x01, 0xEB.toByte(),
            0xE0.toByte(), 0x17, 0x9F.toByte(), 0x1A,
        ),
        byteArrayOf(
            0xFE.toByte(), 0x4F, 0xBE.toByte(), 0xA9.toByte(), 0xF4.toByte(), 0x57, 0x01, 0xA9.toByte(),
            0xE1.toByte(), 0x01, 0x00, 0xB4.toByte(), 0xF3.toByte(), 0x03, 0x00, 0xAA.toByte(),
            0xE0.toByte(), 0x03, 0x01, 0xAA.toByte(), 0x7B, 0xD6.toByte(), 0xF5.toByte(), 0x97.toByte(),
            0xF4.toByte(), 0x03, 0x00, 0xAA.toByte(), 0xE0.toByte(), 0x03, 0x13, 0xAA.toByte(),
            0xE1.toByte(), 0x03, 0x14, 0xAA.toByte(), 0x35, 0xA3.toByte(), 0xF5.toByte(), 0x97.toByte(),
            0x68, 0x1A, 0x40, 0xF9.toByte(), 0xC8.toByte(), 0x00, 0x00, 0xB4.toByte(),
            0x09, 0x0D, 0x40, 0xF9.toByte(), 0x00, 0x21, 0x40, 0xF9.toByte(),
            0x02, 0x15, 0x40, 0xF9.toByte(), 0xE1.toByte(), 0x03, 0x14, 0xAA.toByte(),
            0x20, 0x01, 0x3F, 0xD6.toByte(), 0xF4.toByte(), 0x57, 0x41, 0xA9.toByte(),
            0xFE.toByte(), 0x4F, 0xC2.toByte(), 0xA8.toByte(), 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
        ),
        true,
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
val deathRoverFreeIapPatch = rawResourcePatch(
    name = "Death Rover Free IAP",
    description = "Every shop item reports as purchased and stays owned without contacting Google Play: cars, upgrades, remove ads and double coins come pre-owned, and credit packs are granted instantly and free when bought instead of opening Google Play billing.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_DEATHROVER)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        for (site in SITES) {
            val at = indexOfUnique(bytes, site.anchor, site.label) + site.anchor.size
            require(at + site.replacement.size <= bytes.size) { "${site.label} writes out of bounds" }
            if (site.requireRetPadding) {
                for (k in site.replacement.indices) {
                    require(bytes[at + k] == RET[k % 4]) {
                        "code cave at ${at + k} is not padding (${site.label})"
                    }
                }
            }
            site.replacement.copyInto(bytes, at)
        }

        soFile.writeBytes(bytes)
    }
}
