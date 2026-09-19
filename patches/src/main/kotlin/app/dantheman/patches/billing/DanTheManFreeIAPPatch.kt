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

        // IAP_Support::DoPurchase: b BillingManagerAndroidWrapper::DoPurchase → ret
        // Skips the JNI call to Java, preventing Google Play billing UI from opening.
        java.nio.ByteBuffer.wrap(bytes, 0x68f4a8, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(0xd65f03c0.toInt())

        // IAP_Support::Update: b.eq empty-queue-return → b success-path
        // When the cloud function queue is empty, jump directly to the success path
        // that sets result = 0 instead of returning early with pending state.
        java.nio.ByteBuffer.wrap(bytes, 0x68fb60, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(0x14000028)

        // IAP_Support::Update: movn w8, #0xffffffff → movz w8, #0x0
        // On cloud function failure, write 0 (success) instead of -1 (failure).
        bytes[0x68fc6f] = 0x52.toByte()

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
