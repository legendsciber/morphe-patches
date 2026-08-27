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
 * isProductOwned(String)Z checks mOwnedProducts HashSet.
 * Body is replaced: always returns true.
 */
object IsProductOwnedFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/CommandThreadsafeModel;",
    name = "isProductOwned",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        string("com.mediocre.smashhit.premium")
    )
)

/**
 * GooglePlaySystem.smali - Ad gating check.
 * OnSyncCompleted()V calls ownsPremiumProduct() to decide whether to load ads.
 * We inject at the start to skip ad loading entirely.
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
