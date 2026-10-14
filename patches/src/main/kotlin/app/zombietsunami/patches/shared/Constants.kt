package app.zombietsunami.patches.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_ZOMBIETSUNAMI = Compatibility(
        name = "Zombie Tsunami",
        packageName = "net.mobigame.zombietsunami",
        appIconColor = 0x6CB41C,
        targets = listOf(
            AppTarget(version = "4.7.3")
        )
    )
}
