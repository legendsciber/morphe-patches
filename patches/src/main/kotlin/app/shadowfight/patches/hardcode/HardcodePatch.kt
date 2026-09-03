package app.shadowfight.patches.hardcode

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.shadowfight.patches.shared.Constants.COMPATIBILITY_SF2
import app.shadowfight.patches.dump.UnityOnCreateFingerprint

@Suppress("unused")
val sfHardcodePatch = rawResourcePatch(
    name = "Shadow Fight 2 Hardcode 999999999",
    description = "Sets 5 ObscuredInt values (BFBONKPKBNL 0x10/0x20, ECENNHNBAME 0x24/0x34/0x48) to 999999999 via direct memory. See OFFSETS.md (local only).",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        val soFile = get("lib/arm64-v8a/libShadowHardcode.so", true)
        val soBytes = HardcodeSoBytes.part0() + HardcodeSoBytes.part1() + HardcodeSoBytes.part2() + HardcodeSoBytes.part3() + HardcodeSoBytes.part4() + HardcodeSoBytes.part5() + HardcodeSoBytes.part6() + HardcodeSoBytes.part7() + HardcodeSoBytes.part8() + HardcodeSoBytes.part9()
        soFile.writeBytes(soBytes)
    }
}

@Suppress("unused")
val sfHardcodeTriggerPatch = bytecodePatch(
    name = "Shadow Fight 2 Hardcode Trigger",
    description = "Triggers ShadowHardcode via UnityPlayerActivity.onCreate",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        val idx = UnityOnCreateFingerprint.instructionMatches[0].index + 1
        UnityOnCreateFingerprint.method.addInstructions(idx, """
            const-string v0, "ShadowHardcode"
            invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
        """.trimIndent())
    }
}
