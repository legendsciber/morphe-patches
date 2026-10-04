package app.slingdrift.patches.ad

import app.morphe.patcher.patch.rawResourcePatch
import app.slingdrift.patches.shared.Constants.COMPATIBILITY_SLINGDRIFT

private class Site(
    val label: String,
    val anchor: ByteArray,
    val replacement: ByteArray,
)

private val FORCE_TRUE = byteArrayOf(
    0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val FORCE_FALSE = byteArrayOf(
    0xE0.toByte(), 0x03, 0x1F, 0x2A, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val SITES = listOf(
    Site(
        label = "GameData.HasNoAds",
        anchor = byteArrayOf(
            0xFE.toByte(), 0x0F, 0x1E, 0xF8.toByte(), 0xF4.toByte(), 0x4F, 0x01, 0xA9.toByte(),
            0xD3.toByte(), 0xF2.toByte(), 0x00, 0xF0.toByte(), 0x74, 0xE1.toByte(), 0x00,
            0x90.toByte(), 0x68, 0x2A, 0x6B, 0x39,
        ),
        replacement = FORCE_TRUE,
    ),
    Site(
        label = "AdsManager.TryShowInterstitial",
        anchor = byteArrayOf(
            0xFE.toByte(), 0x57, 0xBE.toByte(), 0xA9.toByte(), 0xF4.toByte(), 0x4F, 0x01,
            0xA9.toByte(), 0xD5.toByte(), 0xF8.toByte(), 0x00, 0x90.toByte(), 0xF3.toByte(),
            0x03, 0x01, 0xAA.toByte(), 0xF4.toByte(), 0x03, 0x00, 0xAA.toByte(), 0xA8.toByte(),
            0x1A, 0x50, 0x39,
        ),
        replacement = FORCE_FALSE,
    ),
    Site(
        label = "AdsManager.CanShowRewarded",
        anchor = byteArrayOf(
            0xFE.toByte(), 0x0F, 0x1B, 0xF8.toByte(), 0xFA.toByte(), 0x67, 0x01, 0xA9.toByte(),
            0xF8.toByte(), 0x5F, 0x02, 0xA9.toByte(), 0xF6.toByte(), 0x57, 0x03, 0xA9.toByte(),
            0xF4.toByte(), 0x4F, 0x04, 0xA9.toByte(), 0xD5.toByte(), 0xF8.toByte(), 0x00,
            0x90.toByte(),
        ),
        replacement = FORCE_FALSE,
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
val slingDriftAdFreePatch = rawResourcePatch(
    name = "Sling Drift Ad Free",
    description = "Banners, interstitials and rewarded video offers are switched off, and the game stops requesting ads on startup, so it launches and runs without touching any ad network.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SLINGDRIFT)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        for (site in SITES) {
            site.replacement.copyInto(bytes, indexOfUnique(bytes, site.anchor, site.label))
        }

        soFile.writeBytes(bytes)
    }
}
