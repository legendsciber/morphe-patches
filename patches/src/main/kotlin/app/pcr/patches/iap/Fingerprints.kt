package app.pcr.patches.iap

import app.morphe.patcher.Fingerprint

object PurchaseStateFingerprint : Fingerprint(
    definingClass = "Lcom/StudioFurukawa/PixelCarRacer/GooglePlayBilling;",
    name = "GPBilling_Purchase_GetState",
    returnType = "D",
    parameters = listOf("Ljava/lang/String;")
)

object VerifySignatureFingerprint : Fingerprint(
    definingClass = "Lcom/StudioFurukawa/PixelCarRacer/GooglePlayBilling;",
    name = "GPBilling_Purchase_VerifySignature",
    returnType = "D",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;")
)
