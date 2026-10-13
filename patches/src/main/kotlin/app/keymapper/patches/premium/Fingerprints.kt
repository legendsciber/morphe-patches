package app.keymapper.patches.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

object EntitlementCheckFingerprint : Fingerprint(
    definingClass = "Lj05;",
    name = "p",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf(
        "Lcom/revenuecat/purchases/CustomerInfo;",
        "Lt65;",
    ),
    filters = listOf(
        methodCall(smali = "Lcom/revenuecat/purchases/CustomerInfo;->getEntitlements()Lcom/revenuecat/purchases/EntitlementInfos;"),
        methodCall(smali = "Lcom/revenuecat/purchases/EntitlementInfo;->isActive()Z"),
    ),
)

object CustomerInfoQueryFingerprint : Fingerprint(
    definingClass = "Lj05;",
    name = "c",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lt65;", "Lq61;"),
    filters = listOf(
        methodCall(smali = "Lj05;->p(Lcom/revenuecat/purchases/CustomerInfo;Lt65;)Z"),
    ),
)

object PurchaseStateSeedFingerprint : Fingerprint(
    definingClass = "Lj05;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Lh81;"),
    filters = listOf(
        methodCall(smali = "Lz80;->j(Ljava/lang/Object;)Lhx5;"),
    ),
)
