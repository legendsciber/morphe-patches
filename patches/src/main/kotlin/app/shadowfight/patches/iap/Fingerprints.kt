package app.shadowfight.patches.iap

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

// FCMNekkiUnityPlayerActivity.onCreate icindeki
// NekkiUnityPlayerActivity.onCreate cagrisini hedef al
object OnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/nekki/utils/activity/FCMNekkiUnityPlayerActivity;",
    name = "onCreate",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PROTECTED),
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/nekki/utils/activity/NekkiUnityPlayerActivity;",
            name = "onCreate"
        )
    )
)
