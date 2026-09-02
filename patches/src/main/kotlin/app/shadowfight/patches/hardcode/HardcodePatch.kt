package app.shadowfight.patches.hardcode

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.shadowfight.patches.shared.Constants.COMPATIBILITY_SF2
import app.shadowfight.patches.cheats.UnityOnCreateFingerprint

@Suppress("unused")
val sfHardcodePatch = rawResourcePatch(
    name = "Shadow Fight 2 Hardcode 999999999",
    description = "Direct memory hardcode for BFBONKPKBNL/ECENNHNBAME ObscuredInt (Gems/Coins) to 999999999. No runtime scan.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        val soFile = get("assets/libShadowHardcode.so", true)
        val soBytes = HardcodeSoBytes.part0() + HardcodeSoBytes.part1() + HardcodeSoBytes.part2() + HardcodeSoBytes.part3() + HardcodeSoBytes.part4() + HardcodeSoBytes.part5() + HardcodeSoBytes.part6() + HardcodeSoBytes.part7() + HardcodeSoBytes.part8() + HardcodeSoBytes.part9()
        soFile.writeBytes(soBytes)
        val dexFile = get("classes10.dex", true)
        val dexBytes = HardcodeHelperBytes.part0() + HardcodeHelperBytes.part1()
        dexFile.writeBytes(dexBytes)
    }
}

@Suppress("unused")
val sfHardcodeTriggerPatch = bytecodePatch(
    name = "Shadow Fight 2 Hardcode Trigger",
    description = "Triggers hardcode via UnityPlayerActivity.onCreate",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        val idx = UnityOnCreateFingerprint.instructionMatches[0].index + 1
        UnityOnCreateFingerprint.method.addInstructions(idx, """
            invoke-static {p0}, Lhelper/HardcodeHelper;->load(Landroid/content/Context;)V
        """.trimIndent())
    }
}
