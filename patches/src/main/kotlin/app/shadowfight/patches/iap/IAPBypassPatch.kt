package app.shadowfight.patches.iap

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.shadowfight.patches.shared.Constants.COMPATIBILITY_SF2
import app.shadowfight.patches.iap.IAPBypassFingerprint

@Suppress("unused")
val sfIAPBypassPatch = rawResourcePatch(
    name = "Shadow Fight 2 IAP Bypass",
    description = "Bypasses in-app purchases.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        val soFile = get("lib/arm64-v8a/libShadowHardcode.so", true)
        val soBytes = IAPBypassSoBytes.part0() + IAPBypassSoBytes.part1() + IAPBypassSoBytes.part2() + IAPBypassSoBytes.part3()
        soFile.writeBytes(soBytes)
    }
}

@Suppress("unused")
val sfIAPBypassTriggerPatch = bytecodePatch(
    name = "Shadow Fight 2 IAP Bypass Trigger",
    description = "Loads IAP bypass native library.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        val idx = IAPBypassFingerprint.instructionMatches[0].index + 1
        IAPBypassFingerprint.method.addInstructions(idx, """
            const-string v0, "ShadowHardcode"
            invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
        """.trimIndent())
    }
}

/**
 * Shadow Fight 2 IAP Bypass — Smali-only version
 *
 * Intercepts zzcc.launchBillingFlow() (the obfuscated BillingClientImpl
 * subclass that actually launches Google Play billing) and replaces it
 * with a fake purchase flow:
 *
 * 1. Extract product ID from BillingFlowParams.zzf (ProductDetailsParams)
 * 2. Build a fake Purchase JSON with the product ID
 * 3. Create a Purchase object from the fake JSON
 * 4. Get the PurchasesUpdatedListener from BillingClientImpl.zze.zzb
 * 5. Call listener.onPurchasesUpdated(OK, [fakePurchase])
 * 6. Return OK BillingResult
 *
 * This triggers the game's normal purchase completion flow (C# callback
 * chain → PurchasingManager → item delivery) without opening Google Play.
 *
 * The method has .locals 3 (v0-v2), which is sufficient for this code.
 * The injected code ends with return-object, so the original method body
 * becomes dead code.
 */
@Suppress("unused")
val sfIAPBypassSmaliPatch = bytecodePatch(
    name = "Shadow Fight 2 IAP Bypass (Smali)",
    description = "Bypasses in-app purchases via smali patching. " +
        "Intercepts launchBillingFlow and triggers fake purchase callback.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        IAPBypassSmaliFingerprint.method.addInstructionsWithLabels(0, """
            # === MORPHE IAP BYPASS PATCH ===
            # Extract product ID from BillingFlowParams.zzf (ArrayList<ProductDetailsParams>)
            iget-object v0, p2, Lcom/android/billingclient/api/BillingFlowParams;->zzf:Ljava/util/ArrayList;

            if-eqz v0, :fallback_error

            invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

            move-result v1

            if-lez v1, :fallback_error

            const/4 v1, 0x0

            invoke-virtual {v0, v1}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

            move-result-object v0

            check-cast v0, Lcom/android/billingclient/api/BillingFlowParams\$ProductDetailsParams;

            iget-object v0, v0, Lcom/android/billingclient/api/BillingFlowParams\$ProductDetailsParams;->zza:Lcom/android/billingclient/api/ProductDetails;

            invoke-virtual {v0}, Lcom/android/billingclient/api/ProductDetails;->getProductId()Ljava/lang/String;

            move-result-object v0

            # Build fake purchase JSON
            new-instance v1, Ljava/lang/StringBuilder;

            invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

            const-string v2, "{\"orderId\":\"morphe_bypass\",\"packageName\":\"com.nekki.shadowfight\",\"productIds\":[\""

            invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

            invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

            const-string v2, "\"],\"purchaseTime\":0,\"purchaseState\":0,\"purchaseToken\":\"morphe_bypass_token\"}"

            invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

            invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

            move-result-object v0

            # Create Purchase(jsonData, signature)
            new-instance v1, Lcom/android/billingclient/api/Purchase;

            const-string v2, ""

            invoke-direct {v1, v0, v2}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V

            # Create ArrayList<Purchase> with our fake purchase
            new-instance v0, Ljava/util/ArrayList;

            invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

            invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

            # Get PurchasesUpdatedListener from this.zze (zzn).zzd()
            iget-object v1, p0, Lcom/android/billingclient/api/BillingClientImpl;->zze:Lcom/android/billingclient/api/zzn;

            if-eqz v1, :fallback_error

            invoke-virtual {v1}, Lcom/android/billingclient/api/zzn;->zzd()Lcom/android/billingclient/api/PurchasesUpdatedListener;

            move-result-object v1

            if-eqz v1, :fallback_error

            # Get OK BillingResult (responseCode=0)
            sget-object v2, Lcom/android/billingclient/api/zzcj;->zzl:Lcom/android/billingclient/api/BillingResult;

            # Call listener.onPurchasesUpdated(OK_result, [fake_purchase])
            invoke-interface {v1, v2, v0}, Lcom/android/billingclient/api/PurchasesUpdatedListener;->onPurchasesUpdated(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V

            return-object v2

            # Fallback: return OK result if anything fails
            :fallback_error
            sget-object v0, Lcom/android/billingclient/api/zzcj;->zzl:Lcom/android/billingclient/api/BillingResult;
            return-object v0
            nop
        """.trimIndent())
    }
}
