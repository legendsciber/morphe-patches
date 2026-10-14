package app.deathrover.patches.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_DEATHROVER = Compatibility(
        name = "Death Rover",
        packageName = "com.tranquility.colony",
        appIconColor = 0x303048,
        targets = listOf(
            AppTarget(version = "2.7.3")
        )
    )
}
