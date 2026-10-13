package app.newstarsoccer.patches.billing

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

object PurchaseFingerprint : Fingerprint(
    definingClass = "Lcom/newstargames/newstarsoccer/GooglePlayBB;",
    name = "Purchase",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(smali = "Lcom/newstargames/newstarsoccer/GooglePlayInvent;->GetDetails(Ljava/lang/String;)Lcom/android/billingclient/api/ProductDetails;"),
        methodCall(smali = "Lcom/newstargames/newstarsoccer/GooglePlayBB\$5;-><init>(Lcom/newstargames/newstarsoccer/GooglePlayBB;Lcom/android/billingclient/api/ProductDetails;Landroid/app/Activity;)V"),
    ),
)

object ConsumeFingerprint : Fingerprint(
    definingClass = "Lcom/newstargames/newstarsoccer/GooglePlayBB;",
    name = "Consume",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(smali = "Lcom/newstargames/newstarsoccer/GooglePlayBB\$6;-><init>(Lcom/newstargames/newstarsoccer/GooglePlayBB;Ljava/lang/String;)V"),
        methodCall(smali = "Landroid/app/Activity;->runOnUiThread(Ljava/lang/Runnable;)V"),
    ),
)
