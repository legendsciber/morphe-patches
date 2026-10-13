package app.keymapper.patches.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.keymapper.patches.shared.Constants.COMPATIBILITY_KEYMAPPER

@Suppress("unused")
val keyMapperPremiumPatch = bytecodePatch(
    name = "Premium",
    description = "Unlocks the floating buttons and assistant trigger features by granting both purchase entitlements, keeping them available even offline where the purchase state cannot be fetched.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_KEYMAPPER)

    execute {
        EntitlementCheckFingerprint.method.addInstructions(
            0,
            """
                const/4 p0, 0x1
                return p0
            """.trimIndent(),
        )

        CustomerInfoQueryFingerprint.method.addInstructions(
            0,
            """
                new-instance v0, Lvz5;
                sget-object v1, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-direct {v0, v1}, Lvz5;-><init>(Ljava/lang/Object;)V
                return-object v0
            """.trimIndent(),
        )

        PurchaseStateSeedFingerprint.method.addInstructions(
            PurchaseStateSeedFingerprint.instructionMatches[0].index,
            """
                new-instance v1, Ljava/util/HashSet;
                invoke-direct {v1}, Ljava/util/HashSet;-><init>()V
                sget-object v3, Lt65;->l:Lt65;
                invoke-virtual {v1, v3}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z
                move-result v3
                sget-object v3, Lt65;->m:Lt65;
                invoke-virtual {v1, v3}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z
                move-result v3
                new-instance v3, Ljava/util/HashMap;
                invoke-direct {v3}, Ljava/util/HashMap;-><init>()V
                new-instance v4, Lv65;
                invoke-direct {v4, v1, v3}, Lv65;-><init>(Ljava/util/Set;Ljava/util/Map;)V
                new-instance v1, Lvz5;
                invoke-direct {v1, v4}, Lvz5;-><init>(Ljava/lang/Object;)V
                new-instance v3, Lax5;
                invoke-direct {v3, v1}, Lax5;-><init>(Ljava/lang/Object;)V
                move-object v1, v3
            """.trimIndent(),
        )
    }
}
