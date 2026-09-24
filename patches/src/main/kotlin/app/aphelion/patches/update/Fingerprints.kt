package app.aphelion.patches.update

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object ForceUpdateVersionCheckFingerprint : Fingerprint(
    definingClass = "Lz61;",
    name = "c",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        string("android_min_supported_version_code"),
        string("android_force_update_message"),
        literal(0x31, opcodes = listOf(Opcode.CONST_WIDE_16)),
    )
)
