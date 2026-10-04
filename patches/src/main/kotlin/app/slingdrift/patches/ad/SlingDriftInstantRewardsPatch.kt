package app.slingdrift.patches.ad

import app.morphe.patcher.patch.rawResourcePatch
import app.slingdrift.patches.shared.Constants.COMPATIBILITY_SLINGDRIFT

private class RewardSite(
    val label: String,
    val anchor: ByteArray,
    val replacement: ByteArray,
)

private class Stub(
    val label: String,
    val offset: Int,
    val bytes: ByteArray,
)

private val FORCE_TRUE = byteArrayOf(
    0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val RET = byteArrayOf(
    0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private const val CAVE_OFFSET = 0x01BA8D68
private const val CAVE_FIRST_STUB = 20
private const val STUB_SIZE = 12

private val STUBS = listOf(
    Stub(
        label = "RubyBonusButton.OnTap",
        offset = CAVE_OFFSET + 20,
        bytes = byteArrayOf(
            0xE7.toByte(), 0x8C.toByte(), 0xEF.toByte(), 0x97.toByte(), 0xE9.toByte(),
            0x8C.toByte(), 0xEF.toByte(), 0x97.toByte(), 0xF9.toByte(), 0x89.toByte(),
            0xEF.toByte(), 0x17,
        ),
    ),
    Stub(
        label = "UpgradeButton.OnButtonClick",
        offset = CAVE_OFFSET + 32,
        bytes = byteArrayOf(
            0xFA.toByte(), 0x03, 0xF0.toByte(), 0x97.toByte(), 0xFC.toByte(), 0x03,
            0xF0.toByte(), 0x97.toByte(), 0xFA.toByte(), 0x00, 0xF0.toByte(), 0x17,
        ),
    ),
    Stub(
        label = "EndGameRewardMultiplierButton.OnTap",
        offset = CAVE_OFFSET + 44,
        bytes = byteArrayOf(
            0x16, 0x12, 0xF0.toByte(), 0x97.toByte(), 0x18, 0x12, 0xF0.toByte(), 0x97.toByte(),
            0x79, 0x10, 0xF0.toByte(), 0x17,
        ),
    ),
    Stub(
        label = "QuestsPopUp.TryRerollDaily",
        offset = CAVE_OFFSET + 56,
        bytes = byteArrayOf(
            0x52, 0x3B, 0xF0.toByte(), 0x97.toByte(), 0x54, 0x3B, 0xF0.toByte(), 0x97.toByte(),
            0x47, 0x3A, 0xF0.toByte(), 0x17,
        ),
    ),
    Stub(
        label = "ContinuePanel.ContinueAdsButtonPressed",
        offset = CAVE_OFFSET + 68,
        bytes = byteArrayOf(
            0xA1.toByte(), 0xCF.toByte(), 0xEE.toByte(), 0x97.toByte(), 0xA3.toByte(),
            0xCF.toByte(), 0xEE.toByte(), 0x97.toByte(), 0x8A.toByte(), 0xCC.toByte(),
            0xEE.toByte(), 0x17,
        ),
    ),
    Stub(
        label = "ContinuePanel.ContinueButtonPressed",
        offset = CAVE_OFFSET + 80,
        bytes = byteArrayOf(
            0x9E.toByte(), 0xCF.toByte(), 0xEE.toByte(), 0x97.toByte(), 0xA0.toByte(),
            0xCF.toByte(), 0xEE.toByte(), 0x97.toByte(), 0x02, 0xD0.toByte(), 0xEE.toByte(),
            0x17,
        ),
    ),
)

private val SITES = listOf(
    RewardSite(
        label = "RubyBonusButton.OnTap",
        anchor = byteArrayOf(
            0x25, 0x66, 0x2D, 0x94.toByte(), 0xC0.toByte(), 0x04, 0x00, 0xB4.toByte(),
            0xE1.toByte(), 0x03, 0x14, 0xAA.toByte(), 0xE2.toByte(), 0x03, 0x1F, 0xAA.toByte(),
        ),
        replacement = byteArrayOf(
            0x06, 0x76, 0x10, 0x14,
        ),
    ),
    RewardSite(
        label = "UpgradeButton.OnButtonClick",
        anchor = byteArrayOf(
            0xB5.toByte(), 0x0A, 0x47, 0xF9.toByte(), 0xA1.toByte(), 0x02, 0x40, 0xF9.toByte(),
        ),
        replacement = byteArrayOf(
            0x05, 0xFF.toByte(), 0x0F, 0x14,
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
            0x86.toByte(), 0xEF.toByte(), 0x0F, 0x14,
        ),
    ),
    RewardSite(
        label = "QuestsPopUp.TryRerollDaily",
        anchor = byteArrayOf(
            0x73, 0x0A, 0x47, 0xF9.toByte(), 0x61, 0x02, 0x40, 0xF9.toByte(),
        ),
        replacement = byteArrayOf(
            0xB8.toByte(), 0xC5.toByte(), 0x0F, 0x14,
        ),
    ),
    RewardSite(
        label = "ContinuePanel.ContinueAdsButtonPressed",
        anchor = byteArrayOf(
            0x88.toByte(), 0x23, 0x2E, 0x94.toByte(), 0x40, 0x0A, 0x00, 0xB4.toByte(),
            0x81.toByte(), 0x02, 0x40, 0xF9.toByte(), 0xE2.toByte(), 0x03, 0x1F, 0xAA.toByte(),
        ),
        replacement = byteArrayOf(
            0x75, 0x33, 0x11, 0x14,
        ),
    ),
    RewardSite(
        label = "ContinuePanel.ContinueButtonPressed",
        anchor = byteArrayOf(
            0x0D, 0x20, 0x2E, 0x94.toByte(), 0x40, 0x0A, 0x00, 0xB4.toByte(), 0x81.toByte(),
            0x02, 0x40, 0xF9.toByte(), 0xE2.toByte(), 0x03, 0x1F, 0xAA.toByte(),
        ),
        replacement = byteArrayOf(
            0xFD.toByte(), 0x2F, 0x11, 0x14,
        ),
    ),
    RewardSite(
        label = "AdsManager.CanShowRewarded",
        anchor = byteArrayOf(
            0xFE.toByte(), 0x57, 0xC2.toByte(), 0xA8.toByte(), 0x6B, 0x35, 0x40, 0x14,
        ),
        replacement = FORCE_TRUE,
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
    description = "Ad-gated rewards are granted without watching an ad: continuing after a crash, free upgrades, the end-of-race multiplier, bonus rubies and daily quest rerolls all work instantly and offline.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SLINGDRIFT)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        for (i in STUBS.indices) {
            val o = CAVE_OFFSET + CAVE_FIRST_STUB + i * STUB_SIZE
            require(
                bytes[o] == RET[0] && bytes[o + 1] == RET[1] &&
                    bytes[o + 2] == RET[2] && bytes[o + 3] == RET[3]
            ) { "code cave at $CAVE_OFFSET is not padding" }
        }

        for (stub in STUBS) {
            stub.bytes.copyInto(bytes, stub.offset)
        }

        for (site in SITES) {
            site.replacement.copyInto(bytes, indexOfUnique(bytes, site.anchor, site.label))
        }

        soFile.writeBytes(bytes)
    }
}
