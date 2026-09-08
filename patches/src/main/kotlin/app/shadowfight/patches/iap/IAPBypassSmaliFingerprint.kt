package app.shadowfight.patches.iap

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * zzcc.launchBillingFlow — the concrete implementation that overrides
 * BillingClientImpl.launchBillingFlow(). This is the method invoked at
 * runtime when C# calls launchBillingFlowCpp (JNI), because virtual
 * dispatch routes to zzcc (the only final subclass).
 *
 * Replacing this method body skips the Google Play billing flow entirely
 * and instead creates a fake Purchase, calls PurchasesUpdatedListener
 * directly, and returns OK — so the game's purchase completion flow
 * triggers naturally, delivering items without Google Play.
 *
 * NOTE: zzcc is R8-obfuscated. The class name may change between app
 * versions. If the fingerprint fails to match, update the
 * definingClass descriptor.
 */
object IAPBypassSmaliFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/zzcc;",
    name = "launchBillingFlow",
    returnType = "Lcom/android/billingclient/api/BillingResult;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/android/billingclient/api/BillingFlowParams;"
    )
)
