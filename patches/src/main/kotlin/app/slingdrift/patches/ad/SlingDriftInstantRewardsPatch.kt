package app.slingdrift.patches.ad

import app.morphe.patcher.patch.rawResourcePatch
import app.slingdrift.patches.shared.Constants.COMPATIBILITY_SLINGDRIFT

private class RewardSite(
    val label: String,
    val anchor: ByteArray,
    val replacement: ByteArray,
)

private val FORCE_TRUE = byteArrayOf(
    0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val SITES = listOf(
    RewardSite(
        label = "RubyBonusButton.OnTap",
        anchor = byteArrayOf(
            0x25, 0x66, 0x2D, 0x94.toByte(), 0xC0.toByte(), 0x04, 0x00, 0xB4.toByte(),
            0xE1.toByte(), 0x03, 0x14, 0xAA.toByte(), 0xE2.toByte(), 0x03, 0x1F, 0xAA.toByte(),
        ),
        replacement = byteArrayOf(
            0xED.toByte(), 0x02, 0x00, 0x94.toByte(),
        ),
    ),
    RewardSite(
        label = "UpgradeButton.OnButtonClick",
        anchor = byteArrayOf(
            0xB5.toByte(), 0x0A, 0x47, 0xF9.toByte(), 0xA1.toByte(), 0x02, 0x40, 0xF9.toByte(),
        ),
        replacement = byteArrayOf(
            0xFF.toByte(), 0x02, 0x00, 0x94.toByte(),
        ),
    ),
    RewardSite(
        label = "EndGameRewardMultiplierButton.OnTap",
        anchor = byteArrayOf(
            0x9F.toByte(), 0xDF.toByte(), 0x2C, 0x94.toByte(), 0xA0.toByte(), 0x06, 0x00,
            0xB4.toByte(), 0x81.toByte(), 0x02, 0x40, 0xF9.toByte(), 0xE2.toByte(), 0x03, 0x1F,
            0xAA.toByte(),
        ),
        replacement = byteArrayOf(
            0x9C.toByte(), 0x01, 0x00, 0x94.toByte(),
        ),
    ),
    RewardSite(
        label = "QuestsPopUp.TryRerollDaily",
        anchor = byteArrayOf(
            0x73, 0x0A, 0x47, 0xF9.toByte(), 0x61, 0x02, 0x40, 0xF9.toByte(),
        ),
        replacement = byteArrayOf(
            0x0A, 0x01, 0x00, 0x94.toByte(),
        ),
    ),
    RewardSite(
        label = "ContinuePanel.ContinueAdsButtonPressed",
        anchor = byteArrayOf(
            0x88.toByte(), 0x23, 0x2E, 0x94.toByte(), 0x40, 0x0A, 0x00, 0xB4.toByte(),
            0x81.toByte(), 0x02, 0x40, 0xF9.toByte(), 0xE2.toByte(), 0x03, 0x1F, 0xAA.toByte(),
        ),
        replacement = byteArrayOf(
            0x16, 0x03, 0x00, 0x94.toByte(),
        ),
    ),
    RewardSite(
        label = "ContinuePanel.ContinueButtonPressed",
        anchor = byteArrayOf(
            0x0D, 0x20, 0x2E, 0x94.toByte(), 0x40, 0x0A, 0x00, 0xB4.toByte(), 0x81.toByte(),
            0x02, 0x40, 0xF9.toByte(), 0xE2.toByte(), 0x03, 0x1F, 0xAA.toByte(),
        ),
        replacement = byteArrayOf(
            0x9B.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x97.toByte(),
        ),
    ),
    RewardSite(
        label = "AdsManager.CanShowRewarded",
        anchor = byteArrayOf(
            0xFE.toByte(), 0x57, 0xC2.toByte(), 0xA8.toByte(), 0x6B, 0x35, 0x40, 0x14,
        ),
        replacement = byteArrayOf(
            0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
        ),
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
val slingDriftInstantRewardsPatch = rawResourcePatch(
    name = "Sling Drift Instant Rewards",
    description = "Rewarded video rewards are granted instantly without playing an ad: continuing after a crash, free upgrades, the end-of-race multiplier, bonus rubies and daily quest rerolls all work offline.",
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
