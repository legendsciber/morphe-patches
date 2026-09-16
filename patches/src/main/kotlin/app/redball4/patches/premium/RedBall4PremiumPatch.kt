package app.redball4.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.redball4.patches.shared.Constants.COMPATIBILITY_REDBALL4

@Suppress("unused")
val redBall4AddNativeLib = rawResourcePatch(
    name = "RB4 Add Hook Native Lib",
    description = "Adds legendsciber.so to APK for runtime IL2CPP premium, no-ads and skin unlock hooking.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_REDBALL4)
    execute {
        val soFile = get("lib/arm64-v8a/liblegendsciber.so", true)
        soFile.writeBytes(SoBytes.bytes())
    }
}

@Suppress("unused")
val redBall4PremiumUnlock = bytecodePatch(
    name = "RB4 Premium & Skin Unlock",
    description = "Unlocks premium, removes ads and unlocks all ball skins via native IL2CPP hook.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_REDBALL4)
    execute {
        val targetMethod = OnCreateFingerprint.method
        val idx = OnCreateFingerprint.instructionMatches[0].index + 1

        targetMethod.addInstructions(idx, """
            const-string v1, "legendsciber"
            invoke-static {v1}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
        """.trimIndent())
    }
}
