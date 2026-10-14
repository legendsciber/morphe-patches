package app.deathrover.patches.billing

import app.morphe.patcher.patch.rawResourcePatch
import app.deathrover.patches.shared.Constants.COMPATIBILITY_DEATHROVER
import app.deathrover.patches.shared.RawPatchSite
import app.deathrover.patches.shared.applySites

// Rover upgrades are deliberately not listed here: ProgressManager.BuyUpgrade pays for them with
// in-game coins (GetCurrencyBalance / TakeCurrency), not with an IAP, so making the ownership
// predicate report true would hide the buy button and take the upgrade path away instead of
// unlocking it. Those stay purchasable with coins the player earns.
private val SITES = listOf(
    RawPatchSite(
        "IAPController.IsProductPurchased",
        byteArrayOf(0x60, 0x00, 0x1F, 0xD6.toByte(), 0x9E.toByte(), 0x18, 0xFB.toByte(), 0x97.toByte()),
        byteArrayOf(0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte()),
    ),
    RawPatchSite(
        "IAPController.BuyItem",
        byteArrayOf(
            0xE0.toByte(), 0x03, 0x13, 0xAA.toByte(), 0xF4.toByte(), 0x4F, 0x42, 0xA9.toByte(),
            0xF6.toByte(), 0x57, 0x41, 0xA9.toByte(), 0xFE.toByte(), 0x5F, 0xC3.toByte(), 0xA8.toByte(),
            0x05, 0x00, 0x00, 0x14, 0xF4.toByte(), 0x4F, 0x42, 0xA9.toByte(),
            0xF6.toByte(), 0x57, 0x41, 0xA9.toByte(), 0xFE.toByte(), 0x5F, 0xC3.toByte(), 0xA8.toByte(),
            0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
        ),
        byteArrayOf(0xC9.toByte(), 0x5B, 0x0A, 0x14),
    ),
    RawPatchSite(
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
        requireRetPadding = true,
    ),
)

@Suppress("unused")
val deathRoverFreeIapPatch = rawResourcePatch(
    name = "Death Rover Free IAP",
    description = "Every shop item reports as purchased and stays owned without contacting Google Play: cars, remove ads and double coins come pre-owned, and credit packs are granted instantly and free when bought instead of opening Google Play billing. Rover upgrades are left untouched, so those stay purchasable with the coins you earn.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_DEATHROVER)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        applySites(bytes, SITES)

        soFile.writeBytes(bytes)
    }
}
