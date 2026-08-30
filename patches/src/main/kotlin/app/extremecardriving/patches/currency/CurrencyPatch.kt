package app.extremecardriving.patches.currency

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.extremecardriving.patches.shared.Constants.COMPATIBILITY_ECD

@Suppress("unused")
val ecdCurrencyPatch = bytecodePatch(
    name = "Extreme Car Driving Unlimited Currencies",
    description = "Sets all in-game currencies (diamonds, coins, upgrade points) to 999,999,999 via IL2CPP API.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ECD)

    dependsOn(ecdAddNativeLibPatch)

    execute {
        OnCreateFingerprint.method.addInstructions(0, """
            const-string v0, "currencyhack"
            invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
        """.trimIndent())
    }
}

@Suppress("unused")
val ecdAddNativeLibPatch = rawResourcePatch(
    name = "Extreme Car Driving Add Native Library",
    description = "Adds the currency hack native library to the APK.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ECD)

    execute {
        val inputStream = classLoader.getResourceAsStream("app/extremecardriving/libcurrencyhack.so")
            ?: error("libcurrencyhack.so not found in patch resources")

        val libFile = get("lib/arm64-v8a/libcurrencyhack.so", true)
        libFile.writeBytes(inputStream.readBytes())
        inputStream.close()

        println("Successfully added libcurrencyhack.so to APK")
    }
}
