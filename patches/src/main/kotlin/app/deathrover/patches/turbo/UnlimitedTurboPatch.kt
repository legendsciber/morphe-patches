package app.deathrover.patches.turbo

import app.morphe.patcher.patch.rawResourcePatch
import app.deathrover.patches.shared.Constants.COMPATIBILITY_DEATHROVER
import app.deathrover.patches.shared.RawPatchSite
import app.deathrover.patches.shared.applySites

// RoverController.ProcessTurboLevel computes "level - rate * deltaTime" from [this + 0x2c], the
// current TurboLevel, and stores the result back. InitUpgrades fills both TurboLevel (0x2c) and
// MaxTurbo (0x3c) from the same source ([_currentTurbo + 0x28]), so the gauge starts full and this
// method is what empties it. Retargeting that one load at MaxTurbo makes every frame recompute
// MaxTurbo - rate * deltaTime instead, which is the maximum minus a single frame of consumption:
// the gauge never runs down and the turbo stays engaged for as long as it is held. The null guard
// above it and the "turbo level > 0" check in Update read the real fields, so a rover with no turbo
// upgrade still has no turbo.
private val SITES = listOf(
    RawPatchSite(
        "RoverController.ProcessTurboLevel",
        byteArrayOf(
            0xC8.toByte(), 0x03, 0x00, 0xB4.toByte(),
            0xF3.toByte(), 0x03, 0x00, 0xAA.toByte(),
        ),
        byteArrayOf(0x08, 0x3C, 0x40, 0xBD.toByte()),
    ),
)

@Suppress("unused")
val deathRoverUnlimitedTurboPatch = rawResourcePatch(
    name = "Death Rover Unlimited Turbo",
    description = "The turbo no longer runs down: while the turbo is engaged the rover keeps refilling its gauge to the maximum instead of spending it, so you can hold the turbo for the whole drive. Cars without a turbo upgrade are unaffected.",
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
