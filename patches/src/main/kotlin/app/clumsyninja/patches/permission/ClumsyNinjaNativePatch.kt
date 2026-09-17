package app.clumsyninja.patches.permission

import app.morphe.patcher.patch.rawResourcePatch
import app.clumsyninja.patches.shared.Constants.COMPATIBILITY_CLUMSYNINJA

private val PATCH = byteArrayOf(
    0x01, 0x00, 0x00, 0x14
)

@Suppress("unused")
val clumsyNinjaNativePatch = rawResourcePatch(
    name = "Source Check Bypass",
    description = "Patches native BootFlow state machine to skip installer/source verification.",
    default = true
) {
    compatibleWith(COMPATIBILITY_CLUMSYNINJA)

    execute {
        val lib = get("lib/arm64-v8a/libClumsyNinja.so", true)
        val bytes = lib.readBytes()
        PATCH.copyInto(bytes, 0x015fc560)
        lib.writeBytes(bytes)
    }
}
