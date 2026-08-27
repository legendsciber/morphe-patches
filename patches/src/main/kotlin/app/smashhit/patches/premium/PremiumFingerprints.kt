package app.smashhit.patches.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * AndroidStore.smali - Checks if user owns premium product.
 * ownsPremiumProduct()Z checks mOwnedProducts for premium or dynamic_premium.
 * Body is replaced: always returns true (simulate premium ownership).
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
 * CommandThreadsafeModel.smali - Checks if products have been synced.
 * hasRefreshedOwnedProducts()Z returns the hasRefreshedOwnedProducts flag.
 * Body is replaced: always returns true (skip purchase sync wait).
 */
object HasRefreshedOwnedProductsFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/CommandThreadsafeModel;",
    name = "hasRefreshedOwnedProducts",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("hasrefreshedownedproducts")
    )
)
