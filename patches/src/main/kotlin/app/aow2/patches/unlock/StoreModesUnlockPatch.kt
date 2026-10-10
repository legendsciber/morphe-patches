package app.aow2.patches.unlock

import app.morphe.patcher.patch.rawResourcePatch
import app.aow2.patches.shared.Constants.COMPATIBILITY_AOW2

private val FORCE_TRUE = byteArrayOf(
    0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

private val SITES = listOf(
    "GlobalVariables.get_IsHackedModeUnlocked" to byteArrayOf(
        0x68, 0xFA.toByte(), 0x35, 0x39, 0x80.toByte(), 0x02, 0x40, 0xF9.toByte(),
        0xF4.toByte(), 0x4F, 0x41, 0xA9.toByte(), 0xFE.toByte(), 0x07, 0x42,
        0xF8.toByte(), 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
    ),
    "GlobalVariables.get_IsGeneralsModeUnlocked" to byteArrayOf(
        0xAC.toByte(), 0xD7.toByte(), 0xFB.toByte(), 0x97.toByte(),
    ),
    "GlobalVariables.get_IsPartyModeUnlocked" to byteArrayOf(
        0xFE.toByte(), 0x8C.toByte(), 0x4C, 0x94.toByte(), 0xF4.toByte(), 0x4F,
        0x41, 0xA9.toByte(), 0x1F, 0x00, 0x00, 0x71, 0xE0.toByte(), 0xD7.toByte(),
        0x9F.toByte(), 0x1A, 0xFE.toByte(), 0x07, 0x42, 0xF8.toByte(),
        0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
    ),
    "GlobalVariables.get_IsGhostModeUnlocked" to byteArrayOf(
        0x21, 0x8D.toByte(), 0x4C, 0x94.toByte(), 0xF4.toByte(), 0x4F, 0x41,
        0xA9.toByte(), 0x1F, 0x00, 0x00, 0x71, 0xE0.toByte(), 0xD7.toByte(),
        0x9F.toByte(), 0x1A, 0xFE.toByte(), 0x07, 0x42, 0xF8.toByte(),
        0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
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
val aow2StoreModesUnlockPatch = rawResourcePatch(
    name = "Age Of War 2 Store Modes Unlock",
    description = "All four paid store modes are owned and unlocked without paying: Hacked Mode, Generals Mode, the Let's Party unit skins and the Age Of Spooky unit skins. The game checks ownership through a PlayerPrefs read, and each mode's unlock query now reports as unlocked, so the store rows show as owned and gameplay applies the modes and skins immediately.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_AOW2)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        for ((label, anchor) in SITES) {
            FORCE_TRUE.copyInto(bytes, indexOfUnique(bytes, anchor, label) + anchor.size)
        }

        soFile.writeBytes(bytes)
    }
}
