package app.shadowfight.patches.iap

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

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
