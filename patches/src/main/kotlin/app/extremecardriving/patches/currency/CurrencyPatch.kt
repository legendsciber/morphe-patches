package app.extremecardriving.patches.currency

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.extremecardriving.patches.shared.Constants.COMPATIBILITY_ECD

@Suppress("unused")
val ecdAddNativeLib = rawResourcePatch(
    name = "Extreme Car Driving Add Native Lib",
    description = "Adds libcurrencyhack.so to assets and helper dex.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ECD)

    execute {
        val soFile = get("assets/libcurrencyhack.so", true)
        val soBytes = SoBytes.part0() + SoBytes.part1() + SoBytes.part2() + SoBytes.part3() + SoBytes.part4() + SoBytes.part5() + SoBytes.part6() + SoBytes.part7() + SoBytes.part8() + SoBytes.part9()
        soFile.writeBytes(soBytes)
        val dexFile = get("classes8.dex", true)
        val dexBytes = HelperDexBytes.part0() + HelperDexBytes.part1()
        dexFile.writeBytes(dexBytes)
    }
}

@Suppress("unused")
val ecdCurrencyPatch = bytecodePatch(
    name = "Extreme Car Driving Unlimited Currencies",
    description = "Sets all in-game currencies (diamonds, coins, upgrade points) to 999,999,999 via IL2CPP API.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ECD)

    execute {
        val idx = OnCreateFingerprint.instructionMatches[2].index + 1
        OnCreateFingerprint.method.addInstructions(idx, """
            invoke-static {p0}, Lhelper/CopyHelper;->load(Landroid/content/Context;)V
        """.trimIndent())
    }
}
