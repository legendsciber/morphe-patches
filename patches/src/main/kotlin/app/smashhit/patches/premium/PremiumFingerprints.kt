package app.smashhit.patches.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * AndroidStore.smali - Master premium ownership check.
 * ownsPremiumProduct()Z checks mOwnedProducts for premium or dynamic_premium.
 * Body is replaced: always returns true.
 */
object OwnsPremiumProductFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AndroidStore;",
    name = "ownsPremiumProduct",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("com.mediocre.smashhit.premium")
    )
)

/**
 * CommandThreadsafeModel.smali - Core product ownership check.
 * isProductOwned(String)Z checks mOwnedProducts HashSet.contains().
 * No string constants inside the method - parameter is passed in.
 * Body is replaced: always returns true.
 */
object IsProductOwnedFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/CommandThreadsafeModel;",
    name = "isProductOwned",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;")
)

/**
 * GooglePlaySystem.smali - Ad gating entry point.
 * OnSyncCompleted()V calls ownsPremiumProduct() then loads ads if not premium.
 * Contains string "GooglePlaySystem.OnSyncCompleted - enter".
 * Body is replaced: return immediately (skip ad loading).
 */
object OnSyncCompletedFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/GooglePlaySystem;",
    name = "OnSyncCompleted",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("GooglePlaySystem.OnSyncCompleted - enter")
    )
)

/**
 * AndroidStore.smali - Purchase flow entry point.
 * startPurchaseFlow(Activity, String)V launches Google Play billing.
 * Contains string "AndroidStore.startPurchaseFlow - enter".
 * Body is replaced: set status to OK (0), error to 0, then return.
 */
object StartPurchaseFlowFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AndroidStore;",
    name = "startPurchaseFlow",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;"),
    filters = listOf(
        string("AndroidStore.startPurchaseFlow - enter")
    )
)

/**
 * CommandHandler.smali - Lambda that returns purchase status.
 * lambda$setupCommands$37 reads purchaseStatusCode AtomicInteger.
 * Contains "CommandHandler.command, storepurchase" nearby.
 * Body is replaced: return "0" (PURCHASE_OK).
 */
object StoreGetStatusFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/CommandHandler;",
    returnType = "Ljava/lang/String;",
    parameters = listOf("[Ljava/lang/String;"),
    filters = listOf(
        string("CommandHandler.command, storegetstatus")
    )
)

/**
 * CommandHandler.smali - Lambda that returns purchase error code.
 * lambda$setupCommands$38 reads purchaseErrorCode AtomicInteger.
 * Body is replaced: return "0" (no error).
 */
object StoreGetErrorFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/CommandHandler;",
    returnType = "Ljava/lang/String;",
    parameters = listOf("[Ljava/lang/String;"),
    filters = listOf(
        string("CommandHandler.command, storegeterror")
    )
)
