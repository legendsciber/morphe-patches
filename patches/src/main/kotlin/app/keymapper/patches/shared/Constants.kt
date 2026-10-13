package app.keymapper.patches.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_KEYMAPPER = Compatibility(
        name = "Key Mapper",
        packageName = "io.github.sds100.keymapper",
        appIconColor = 0x1564BE,
        targets = listOf(
            AppTarget(version = "4.5.0")
        )
    )
}
