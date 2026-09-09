package app.shadowfight.patches.iap

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.shadowfight.patches.shared.Constants.COMPATIBILITY_SF2

/**
 * Shadow Fight 2 IAP Bypass — Quintuple interception (v4)
 *
 * Five interception points work together:
 *
 * 1. BillingClientImpl.launchBillingFlow — prevents Google Play from
 *    opening, extracts product ID, builds fake Purchase, calls
 *    PurchasesUpdatedListener.onPurchasesUpdated directly, returns OK.
 *
 * 2. zzbm.onPurchasesUpdated — intercepts the callback triggered by
 *    step 1, injects fake Purchase into nativeOnPurchasesUpdated so
 *    the C++ layer receives valid purchase data.
 *
 * 3. zzbm.onQueryPurchasesResponse — ensures C++ verification via
 *    queryPurchasesAsync also sees a valid purchase.
 *
 * 4-5. BillingClientImpl.queryPurchasesAsync (both overloads) — returns
 *    fake OK result with empty list to prevent "connection error" when
 *    the game queries purchases without a Google Play connection.
 */
@Suppress("unused")
val sfIAPBypassSmaliPatch = bytecodePatch(
    name = "Shadow Fight 2 IAP Bypass (Smali)",
    description = "Bypasses in-app purchases via smali patching. " +
        "Five interceptions: launchBillingFlow + zzbm callbacks + queryPurchasesAsync.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        // === 1. Intercept launchBillingFlow: prevent Google Play, trigger callback ===
        IAPBypassLaunchBillingFlowFingerprint.method.addInstructionsWithLabels(0, """
            invoke-virtual/range {p2 .. p2}, Lcom/android/billingclient/api/BillingFlowParams;->zzh()Ljava/util/List;
            move-result-object v0
            if-eqz v0, :lbill_fallback
            invoke-interface {v0}, Ljava/util/List;->size()I
            move-result v1
            if-lez v1, :lbill_fallback
            const/4 v1, 0x0
            invoke-interface {v0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;
            move-result-object v0
            check-cast v0, Lcom/android/billingclient/api/BillingFlowParams${'$'}ProductDetailsParams;
            invoke-virtual {v0}, Lcom/android/billingclient/api/BillingFlowParams${'$'}ProductDetailsParams;->zza()Lcom/android/billingclient/api/ProductDetails;
            move-result-object v0
            invoke-virtual {v0}, Lcom/android/billingclient/api/ProductDetails;->getProductId()Ljava/lang/String;
            move-result-object v0
            new-instance v1, Ljava/lang/StringBuilder;
            invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
            const-string v2, "{\"orderId\":\"morphe_bypass\",\"packageName\":\"com.nekki.shadowfight\",\"productIds\":[\""
            invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            const-string v2, "\"],\"purchaseTime\":0,\"purchaseState\":1,\"purchaseToken\":\"morphe_bypass_token\",\"acknowledged\":true}"
            invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            new-instance v1, Lcom/android/billingclient/api/Purchase;
            const-string v2, ""
            invoke-direct {v1, v0, v2}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V
            new-instance v0, Ljava/util/ArrayList;
            invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
            invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
            iget-object v1, p0, Lcom/android/billingclient/api/BillingClientImpl;->zze:Lcom/android/billingclient/api/zzn;
            if-eqz v1, :lbill_fallback
            invoke-virtual {v1}, Lcom/android/billingclient/api/zzn;->zzd()Lcom/android/billingclient/api/PurchasesUpdatedListener;
            move-result-object v1
            if-eqz v1, :lbill_fallback
            sget-object v2, Lcom/android/billingclient/api/zzcj;->zzl:Lcom/android/billingclient/api/BillingResult;
            invoke-interface {v2, v0}, Lcom/android/billingclient/api/PurchasesUpdatedListener;->onPurchasesUpdated(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
            return-object v2
            :lbill_fallback
            sget-object v0, Lcom/android/billingclient/api/zzcj;->zzl:Lcom/android/billingclient/api/BillingResult;
            return-object v0
        """.trimIndent())

        // === 2. Intercept onPurchasesUpdated: inject fake Purchase into native layer ===
        IAPBypassOnPurchasesUpdatedFingerprint.method.addInstructionsWithLabels(0, """
            new-instance v0, Ljava/lang/StringBuilder;
            invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
            const-string v1, "{\"orderId\":\"morphe_bypass\",\"packageName\":\"com.nekki.shadowfight\",\"productIds\":[\"gem_d pack\"],\"purchaseTime\":"
            invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
            move-result-wide v2
            invoke-static {v2, v3}, Ljava/lang/String;->valueOf(J)Ljava/lang/String;
            move-result-object v1
            invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            const-string v1, ",\"purchaseState\":1,\"purchaseToken\":\"morphe_bypass_token\",\"acknowledged\":true}"
            invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            new-instance v1, Lcom/android/billingclient/api/Purchase;
            const-string v2, ""
            invoke-direct {v1, v0, v2}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V
            new-instance v0, Ljava/util/ArrayList;
            invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
            invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
            invoke-virtual {v0}, Ljava/util/ArrayList;->size()I
            move-result v1
            new-array v1, v1, [Lcom/android/billingclient/api/Purchase;
            invoke-virtual {v0, v1}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;
            move-result-object v0
            check-cast v0, [Lcom/android/billingclient/api/Purchase;
            move-object/from16 v3, v0
            const/4 v1, 0x0
            const-string v2, ""
            invoke-static/range {v1 .. v3}, Lcom/android/billingclient/api/zzbm;->nativeOnPurchasesUpdated(ILjava/lang/String;[Lcom/android/billingclient/api/Purchase;)V
            return-void
        """.trimIndent())

        // === 3. Intercept onQueryPurchasesResponse: fake purchase for C++ verification ===
        IAPBypassOnQueryPurchasesResponseFingerprint.method.addInstructionsWithLabels(0, """
            new-instance v0, Ljava/lang/StringBuilder;
            invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
            const-string v1, "{\"orderId\":\"morphe_bypass\",\"packageName\":\"com.nekki.shadowfight\",\"productIds\":[\"gem_d pack\"],\"purchaseTime\":"
            invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
            move-result-wide v2
            invoke-static {v2, v3}, Ljava/lang/String;->valueOf(J)Ljava/lang/String;
            move-result-object v1
            invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            const-string v1, ",\"purchaseState\":1,\"purchaseToken\":\"morphe_bypass_token\",\"acknowledged\":true}"
            invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            new-instance v1, Lcom/android/billingclient/api/Purchase;
            const-string v2, ""
            invoke-direct {v1, v0, v2}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V
            new-instance v0, Ljava/util/ArrayList;
            invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
            invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
            invoke-virtual {v0}, Ljava/util/ArrayList;->size()I
            move-result v1
            new-array v1, v1, [Lcom/android/billingclient/api/Purchase;
            invoke-virtual {v0, v1}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;
            move-result-object v0
            check-cast v0, [Lcom/android/billingclient/api/Purchase;
            move-object/from16 v5, v0
            move-object/from16 v4, p0
            iget-wide v0, v4, Lcom/android/billingclient/api/zzbm;->zza:J
            move-wide v6, v0
            const/4 v3, 0x0
            const-string v4, ""
            invoke-static/range {v3 .. v7}, Lcom/android/billingclient/api/zzbm;->nativeOnQueryPurchasesResponse(ILjava/lang/String;[Lcom/android/billingclient/api/Purchase;J)V
            return-void
        """.trimIndent())

        // === 4. Intercept queryPurchasesAsync(QueryPurchasesParams): prevent connection error ===
        IAPBypassQueryPurchasesAsyncParamsFingerprint.method.addInstructionsWithLabels(0, """
            sget-object v0, Lcom/android/billingclient/api/zzcj;->zzl:Lcom/android/billingclient/api/BillingResult;
            new-instance v1, Ljava/util/ArrayList;
            invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V
            invoke-interface {p2, v0, v1}, Lcom/android/billingclient/api/PurchasesResponseListener;->onQueryPurchasesResponse(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
            return-void
        """.trimIndent())

        // === 5. Intercept queryPurchasesAsync(String): prevent connection error ===
        IAPBypassQueryPurchasesAsyncStringFingerprint.method.addInstructionsWithLabels(0, """
            sget-object v0, Lcom/android/billingclient/api/zzcj;->zzl:Lcom/android/billingclient/api/BillingResult;
            new-instance v1, Ljava/util/ArrayList;
            invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V
            invoke-interface {p2, v0, v1}, Lcom/android/billingclient/api/PurchasesResponseListener;->onQueryPurchasesResponse(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
            return-void
        """.trimIndent())
    }
}
