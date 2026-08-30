package app.pcr.patches.iap

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.pcr.patches.shared.Constants.COMPATIBILITY_PCR

@Suppress("unused")
val pcrIAPBypassPatch = bytecodePatch(
    name = "IAP Bypass",
    description = "Bypasses in-app purchase verification. All purchases are treated as completed.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PCR)

    execute {
        PurchaseProductFingerprint.method.addInstructions(0, """
            const-wide/16 v0, 0x0
            return-wide v0
        """.trimIndent())

        PurchaseSubscriptionFingerprint.method.addInstructions(0, """
            const-wide/16 v0, 0x0
            return-wide v0
        """.trimIndent())

        PurchaseStateFingerprint.method.addInstructions(0, """
            const-wide v0, 0x40c9648000000000L
            return-wide v0
        """.trimIndent())

        VerifySignatureFingerprint.method.addInstructions(0, """
            const-wide/high16 v0, 0x3ff0000000000000L
            return-wide v0
        """.trimIndent())

        IsStoreConnectedFingerprint.method.addInstructions(0, """
            const-wide/high16 v0, 0x3ff0000000000000L
            return-wide v0
        """.trimIndent())

        GetStatusFingerprint.method.addInstructions(0, """
            const-wide/16 v0, 0x0
            return-wide v0
        """.trimIndent())

        PurchaseGetOriginalJsonFingerprint.method.addInstructions(0, """
            new-instance v0, Ljava/lang/StringBuilder;
            invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
            const-string v1, "{\"orderId\":\"MOCK\",\"packageName\":\"com.StudioFurukawa.PixelCarRacer\",\"productId\":\""
            invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            const-string v1, "\",\"purchaseTime\":1234567890,\"purchaseToken\":\"mock_token\",\"purchaseState\":1,\"acknowledged\":false}"
            invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            return-object v0
        """.trimIndent())

        PurchaseGetSignatureFingerprint.method.addInstructions(0, """
            const-string v0, "MOCK_SIGNATURE"
            return-object v0
        """.trimIndent())
    }
}
