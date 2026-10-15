package app.zenpinball.patches.billing

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

private const val STORE = "Lcom/zenstudios/Interfaces/GoogleStoreInterface;"

object PurchaseFingerprint : Fingerprint(
    definingClass = STORE,
    name = "purchase",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "I"),
    filters = listOf(
        methodCall(smali = "Lcom/zenstudios/Interfaces/GoogleStoreInterface\$4;-><init>(Lcom/zenstudios/Interfaces/GoogleStoreInterface;Lcom/android/billingclient/api/ProductDetails;I)V"),
    ),
)

object ConsumeFingerprint : Fingerprint(
    definingClass = STORE,
    name = "consume",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "I"),
    filters = listOf(
        methodCall(smali = "Lcom/zenstudios/Interfaces/GoogleStoreInterface\$5;-><init>(Lcom/zenstudios/Interfaces/GoogleStoreInterface;Lcom/android/billingclient/api/Purchase;Ljava/lang/String;I)V"),
    ),
)
