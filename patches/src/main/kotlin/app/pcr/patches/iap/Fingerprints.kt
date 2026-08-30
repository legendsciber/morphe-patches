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

object PurchaseProductFingerprint : Fingerprint(
    definingClass = "Lcom/StudioFurukawa/PixelCarRacer/GooglePlayBilling;",
    name = "GPBilling_PurchaseProduct",
    returnType = "D",
    parameters = listOf("Ljava/lang/String;")
)

object IsStoreConnectedFingerprint : Fingerprint(
    definingClass = "Lcom/StudioFurukawa/PixelCarRacer/GooglePlayBilling;",
    name = "GPBilling_IsStoreConnected",
    returnType = "D",
    parameters = listOf()
)

object GetStatusFingerprint : Fingerprint(
    definingClass = "Lcom/StudioFurukawa/PixelCarRacer/GooglePlayBilling;",
    name = "GPBilling_GetStatus",
    returnType = "D",
    parameters = listOf()
)
