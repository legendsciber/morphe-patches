package app.zombietsunami.patches.billing

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

private const val BILLING = "Lnet/mobigame/artemis/billing/MobiGooglePlayBilling;"

/**
 * `launchBillingFlow(Activity, String sku, boolean consumable, String... oldSkus)` is the single
 * choke point of the store: `MobiActivity.purchase` -> `purchaseWithGoogle` -> here, and this is
 * the only place in the whole app that reaches `BillingClient.launchBillingFlow`. The hook goes
 * right after the method's own `if-nez p2, :cond_0`, which is what makes `p2` (the sku) provably
 * non-null there, so the inserted call needs no null guard of its own.
 *
 * The first filter pins the method by the string it logs on entry. The second lands on the
 * `m_BillingClient` field read at `:cond_0`, and it is the first such read in the method, so
 * `instructionMatches[1].index` is exactly that instruction.
 *
 * The hook is installed at `index + 1`, never on the read itself: the read is what `:cond_0`
 * points at, and inserting behind a labelled instruction lets the assembler bind the label
 * either to the original instruction or to the inserted block. Only the second reading puts the
 * hook on the purchase path, so anchoring one instruction lower removes the ambiguity - and
 * that instruction is far enough from every branch in the method that no existing offset moves.
 */
object LaunchBillingFlowFingerprint : Fingerprint(
    definingClass = BILLING,
    name = "launchBillingFlow",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;", "Z", "[Ljava/lang/String;"),
    filters = listOf(
        string("MobiGooglePlayBilling launchBillingFlow sku : "),
        fieldAccess(smali = "${BILLING}->m_BillingClient:Lcom/android/billingclient/api/BillingClient;"),
    ),
)
