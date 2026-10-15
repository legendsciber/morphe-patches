package app.smashhit.patches.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patcher.methodCall

object OwnsPremiumProductFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AndroidStore;",
    name = "ownsPremiumProduct",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("com.mediocre.smashhit.premium")
    )
)

object IsProductOwnedFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/CommandThreadsafeModel;",
    name = "isProductOwned",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Ljava/util/HashSet;",
            name = "contains"
        )
    )
)

object OnSyncCompletedFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/GooglePlaySystem;",
    name = "OnSyncCompleted",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("GooglePlaySystem.OnSyncCompleted - enter")
    )
)

object StartPurchaseFlowFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/AndroidStore;",
    name = "startPurchaseFlow",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;"),
    filters = listOf(
        string("AndroidStore.startPurchaseFlow - enter")
    )
)

object HasRefreshedOwnedProductsFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/CommandHandler;",
    name = "lambda\$setupCommands\$1\$com-mediocre-smashhit-CommandHandler",
    returnType = "Ljava/lang/String;",
    parameters = listOf("[Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Ljava/util/concurrent/atomic/AtomicBoolean;",
            name = "get"
        )
    )
)

object IsPremiumProductRestoredFingerprint : Fingerprint(
    definingClass = "Lcom/mediocre/smashhit/CommandHandler;",
    name = "lambda\$setupCommands\$41\$com-mediocre-smashhit-CommandHandler",
    returnType = "Ljava/lang/String;",
    parameters = listOf("[Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Ljava/util/concurrent/atomic/AtomicBoolean;",
            name = "get"
        )
    )
)
