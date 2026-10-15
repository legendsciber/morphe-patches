package app.zombietsunami.patches.billing

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string

private const val BILLING = "Lnet/mobigame/artemis/billing/MobiGooglePlayBilling;"

object LaunchBillingFlowFingerprint : Fingerprint(
    definingClass = BILLING,
    name = "launchBillingFlow",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;", "Z", "[Ljava/lang/String;"),
    filters = listOf(
        string("MobiGooglePlayBilling launchBillingFlow sku : "),
        fieldAccess(smali = "${BILLING}->m_BillingClient:Lcom/android/billingclient/api/BillingClient;"),
    ),
)
