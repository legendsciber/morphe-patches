package app.aow2.patches.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_AOW2 = Compatibility(
        name = "Age Of War 2",
        packageName = "com.maxgames.aow2",
        appIconColor = 0xC1682F,
        targets = listOf(
            AppTarget(version = "2026.1.1")
        )
    )
}
