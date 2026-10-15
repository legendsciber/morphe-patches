package app.zenpinball.patches.billing

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zenpinball.patches.shared.Constants.COMPATIBILITY_ZENPINBALL

private const val PURCHASE_SMALI = """
    const-string v0, "GooglePlay"
    const-string v1, "purchase() free IAP: granting without Play Store"
    invoke-static {v0, v1}, Lcom/zenstudios/px/Log;->d(Ljava/lang/String;Ljava/lang/String;)V
    invoke-virtual {p1}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
    move-result-object v0
    new-instance v1, Lorg/json/JSONObject;
    invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
    const-string v2, "productId"
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;
    const-string v2, "purchaseState"
    const/4 v0, 0x0
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;
    const-string v2, "quantity"
    const/4 v0, 0x1
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;
    const-string v2, "acknowledged"
    const/4 v0, 0x1
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;
    const-string v2, "purchaseToken"
    const-string v0, "free-iap"
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;
    invoke-virtual {v1}, Lorg/json/JSONObject;->toString()Ljava/lang/String;
    move-result-object v1
    new-instance v2, Lcom/android/billingclient/api/Purchase;
    const-string v0, ""
    invoke-direct {v2, v1, v0}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V
    iget-object v0, p0, Lcom/zenstudios/Interfaces/GoogleStoreInterface;->m_Inventory:Lcom/zenstudios/store/iab/Inventory;
    invoke-virtual {v0, v2}, Lcom/zenstudios/store/iab/Inventory;->addPurchase(Lcom/android/billingclient/api/Purchase;)V
    new-instance v0, Lcom/zenstudios/Interfaces/PurchaseInfo;
    invoke-virtual {v2}, Lcom/android/billingclient/api/Purchase;->getSkus()Ljava/util/ArrayList;
    move-result-object v1
    const/4 v3, 0x0
    invoke-virtual {v1, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v1
    check-cast v1, Ljava/lang/String;
    invoke-virtual {v2}, Lcom/android/billingclient/api/Purchase;->getOriginalJson()Ljava/lang/String;
    move-result-object v3
    invoke-virtual {v2}, Lcom/android/billingclient/api/Purchase;->getSignature()Ljava/lang/String;
    move-result-object v2
    invoke-direct {v0, v1, v3, v2}, Lcom/zenstudios/Interfaces/PurchaseInfo;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    const/4 v1, 0x0
    invoke-static {p3, v1, v0}, Lcom/zenstudios/px/JniLib;->onRequestCompleted(IILjava/lang/Object;)V
    return-void
"""

private const val CONSUME_SMALI = """
    const-string v0, "GooglePlay"
    const-string v1, "consume() free IAP: releasing local entry"
    invoke-static {v0, v1}, Lcom/zenstudios/px/Log;->d(Ljava/lang/String;Ljava/lang/String;)V
    invoke-virtual {p1}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
    move-result-object v0
    iget-object v1, p0, Lcom/zenstudios/Interfaces/GoogleStoreInterface;->m_Inventory:Lcom/zenstudios/store/iab/Inventory;
    invoke-virtual {v1, v0}, Lcom/zenstudios/store/iab/Inventory;->erasePurchase(Ljava/lang/String;)V
    const/4 v0, 0x0
    const/4 v1, 0x0
    invoke-static {p2, v0, v1}, Lcom/zenstudios/px/JniLib;->onRequestCompleted(IILjava/lang/Object;)V
    return-void
"""

@Suppress("unused")
val zenPinballFreeIapPatch = bytecodePatch(
    name = "Zen Pinball Free IAP",
    description = "Coins, tables and every other store item are granted instantly and free, without launching Google Play billing.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ZENPINBALL)

    execute {
        PurchaseFingerprint.method.let { purchase ->
            purchase.removeInstructions(purchase.implementation!!.instructions.size)
            purchase.addInstructions(0, PURCHASE_SMALI.trimIndent())
        }

        ConsumeFingerprint.method.let { consume ->
            consume.removeInstructions(consume.implementation!!.instructions.size)
            consume.addInstructions(0, CONSUME_SMALI.trimIndent())
        }
    }
}
