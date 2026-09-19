package app.dantheman.patches.billing

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.dantheman.patches.shared.Constants.COMPATIBILITY_DANTHEMAN

private const val BILLING_MANAGER = "Lcom/halfbrick/mortar/BillingManager;"

@Suppress("unused")
val danTheManFreeIAPPatch = rawResourcePatch(
    name = "Dan The Man Free IAP",
    description = "All in-app purchases are granted instantly and free without Google Play billing.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_DANTHEMAN)

    execute {
        val soFile = get("lib/arm64-v8a/libmortargame.so", true)
        val bytes = soFile.readBytes()

        // IAP_Support::DoPurchase: mov w8, #1 → mov w8, #0
        // Set purchase result to 0 (success) immediately instead of 1 (pending).
        // GetLastPurchaseResult reads [0x111c208]; game sees success and grants items.
        bytes[0x68f464] = 0x08.toByte()

        // IAP_Support::DoPurchase: b BillingManagerAndroidWrapper::DoPurchase → ret
        // Skip the JNI call to Java, preventing Google Play billing UI from opening.
        java.nio.ByteBuffer.wrap(bytes, 0x68f4a8, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(0xd65f03c0.toInt())

        soFile.writeBytes(bytes)
    }
}

@Suppress("unused")
val danTheManFreeIAPSmaliPatch = bytecodePatch(
    name = "Dan The Man Free IAP Smali",
    description = "Fake product info: returns $0.00 for all items.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_DANTHEMAN)

    execute {
        GetProductInfoFingerprint.method.addInstructions(0, """
            const/4 v0, 0x0
            const-string v1, "0.00"
            const-string v2, "USD"
            invoke-static {v0, p0, v1, v2}, $BILLING_MANAGER->DetailsCallback(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
            return-void
        """.trimIndent())
    }
}
