package app.aow2.patches.ad

import app.morphe.patcher.patch.rawResourcePatch
import app.aow2.patches.shared.Constants.COMPATIBILITY_AOW2

private class AdSite(
    val label: String,
    val anchor: ByteArray,
    val replacement: ByteArray,
)

private val RET = byteArrayOf(
    0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val NOP = byteArrayOf(
    0x1F, 0x20, 0x03, 0xD5.toByte(),
)

private val SITES = listOf(
    AdSite(
        label = "IronSourceInitilizer.Initilize",
        anchor = byteArrayOf(0x52, 0x41, 0xB1.toByte(), 0x97.toByte()),
        replacement = RET,
    ),
    AdSite(
        label = "AdvertisementManager.Start",
        anchor = byteArrayOf(0x4D, 0x1E, 0x4D, 0x14),
        replacement = RET,
    ),
    AdSite(
        label = "AdvertisementManager.ShowRewarded",
        anchor = byteArrayOf(
            0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(), 0x77, 0x37,
            0xFC.toByte(), 0x97.toByte(),
        ),
        replacement = RET,
    ),
    AdSite(
        label = "AdvertisementManager.Show.ReadyGate",
        anchor = byteArrayOf(0xDC.toByte(), 0xD2.toByte(), 0x4A, 0x94.toByte()),
        replacement = NOP,
    ),
    AdSite(
        label = "AdvertisementManager.ShowAd.NotReadyGate",
        anchor = byteArrayOf(0xA1.toByte(), 0xD2.toByte(), 0x4A, 0x94.toByte()),
        replacement = byteArrayOf(0x21, 0x00, 0x00, 0x14),
    ),
    AdSite(
        label = "PostGameAd.PlayAd.NotReadyGate",
        anchor = byteArrayOf(0x2A, 0xB0.toByte(), 0x4A, 0x94.toByte()),
        replacement = byteArrayOf(0x26, 0x00, 0x00, 0x14),
    ),
    AdSite(
        label = "LevelPlaySample.Start",
        anchor = byteArrayOf(0x9D.toByte(), 0x01, 0x4D, 0x14),
        replacement = RET,
    ),
    AdSite(
        label = "LevelPlaySample.OnEnable",
        anchor = byteArrayOf(0x58, 0x1E, 0xFC.toByte(), 0x97.toByte()),
        replacement = RET,
    ),
    AdSite(
        label = "LevelPlaySample.OnGUI",
        anchor = byteArrayOf(0xA0.toByte(), 0x1C, 0xFC.toByte(), 0x97.toByte()),
        replacement = RET,
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
val aow2RemoveAdsPatch = rawResourcePatch(
    name = "Age Of War 2 Remove Ads",
    description = "Removes every ad: the ironSource and LevelPlay SDK is never initialised, so no interstitial, banner or rewarded video can load or be shown. Ad-gated actions still complete through the game's own no-ad fallback, so post-game navigation and ad-gated rewards proceed instantly without a network connection.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_AOW2)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        for (site in SITES) {
            site.replacement.copyInto(
                bytes,
                indexOfUnique(bytes, site.anchor, site.label) + site.anchor.size,
            )
        }

        soFile.writeBytes(bytes)
    }
}
