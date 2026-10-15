package app.zombietsunami.patches.billing

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string

private const val BILLING = "Lnet/mobigame/artemis/billing/MobiGooglePlayBilling;"

/**
 * `launchBillingFlow(Activity, String sku, boolean consumable, String... oldSkus)` is the single
 * choke point of the store: `MobiActivity.purchase` -> `purchaseWithGoogle` -> here, and this is
 * the only place in the whole app that reaches `BillingClient.launchBillingFlow`. The method
 * begins by branching to `:cond_0` only when the sku is non-null, so every instruction there
 * runs with a non-null `p2` and the inserted call needs no null guard of its own.
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
    // `accessFlags` is deliberately left unset. The patcher compares it against the method's raw
    // dex flags as an exact bitmask, and this method is declared `public varargs`, so its flags
    // are ACC_PUBLIC|ACC_VARARGS rather than ACC_PUBLIC alone. Any shorthand that does not name
    // every set bit fails to match, which makes the constraint capable of only breaking things;
    // the class, name, signature and the two filters already pin down a single method.
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;", "Z", "[Ljava/lang/String;"),
    filters = listOf(
        string("MobiGooglePlayBilling launchBillingFlow sku : "),
        fieldAccess(smali = "${BILLING}->m_BillingClient:Lcom/android/billingclient/api/BillingClient;"),
    ),
)
