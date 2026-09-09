package app.shadowfight.patches.iap

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * BillingClientImpl.launchBillingFlow — the base implementation called
 * at runtime when the game's C++ code invokes launchBillingFlow via JNI.
 * Intercepted to prevent Google Play from opening and instead trigger
 * the purchase callback with a fake Purchase.
 */
object IAPBypassLaunchBillingFlowFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/BillingClientImpl;",
    name = "launchBillingFlow",
    returnType = "Lcom/android/billingclient/api/BillingResult;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/android/billingclient/api/BillingFlowParams;"
    )
)

/**
 * zzbm.onPurchasesUpdated — Unity JNI bridge callback that receives
 * purchase results from Google Play Billing. Intercepted to inject a
 * fake Purchase and call nativeOnPurchasesUpdated directly, bypassing
 * Google Play while triggering the game's C# purchase completion flow.
 */
object IAPBypassOnPurchasesUpdatedFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/zzbm;",
    name = "onPurchasesUpdated",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Lcom/android/billingclient/api/BillingResult;",
        "Ljava/util/List;"
    )
)

/**
 * zzbm.onQueryPurchasesResponse — Unity JNI bridge callback that receives
 * query-purchase results. Intercepted to inject the same fake Purchase so
 * the C++ side sees a valid purchase when verifying via queryPurchasesAsync.
 */
object IAPBypassOnQueryPurchasesResponseFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/zzbm;",
    name = "onQueryPurchasesResponse",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Lcom/android/billingclient/api/BillingResult;",
        "Ljava/util/List;"
    )
)
