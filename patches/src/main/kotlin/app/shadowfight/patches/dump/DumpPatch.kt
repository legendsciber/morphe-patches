package app.shadowfight.patches.dump

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.shadowfight.patches.shared.Constants.COMPATIBILITY_SF2
import app.shadowfight.patches.cheats.UnityOnCreateFingerprint

@Suppress("unused")
val sfDumpPatch = rawResourcePatch(
    name = "Shadow Fight 2 Runtime Dump",
    description = "Dumps all Unity classes/fields with ObscuredInt to logcat for hardcode. Use logcat -s ShadowDump.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        val soFile = get("assets/libShadowDump.so", true)
        val soBytes = DumpSoBytes.part0() + DumpSoBytes.part1() + DumpSoBytes.part2() + DumpSoBytes.part3() + DumpSoBytes.part4() + DumpSoBytes.part5() + DumpSoBytes.part6() + DumpSoBytes.part7() + DumpSoBytes.part8() + DumpSoBytes.part9()
        soFile.writeBytes(soBytes)
    }
}

@Suppress("unused")
val sfDumpTriggerPatch = bytecodePatch(
    name = "Shadow Fight 2 Dump Trigger",
    description = "Triggers ShadowDump via UnityPlayerActivity.onCreate",
    default = true
) {
    compatibleWith(COMPATIBILITY_SF2)
    execute {
        UnityOnCreateFingerprint.method.addInstructions(0, """
            const-string v0, "libShadowDump.so"
            invoke-virtual {p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;
            move-result-object v1
            invoke-virtual {v1, v0}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;
            move-result-object v1
            invoke-static {v1}, Lcom/google/android/gms/common/util/IOUtils;->readInputStreamFully(Ljava/io/InputStream;)[B
            move-result-object v0
            invoke-virtual {v1}, Ljava/io/InputStream;->close()V
            invoke-virtual {p0}, Landroid/content/Context;->getFilesDir()Ljava/io/File;
            move-result-object v1
            new-instance v2, Ljava/io/File;
            const-string v3, "libShadowDump.so"
            invoke-direct {v2, v1, v3}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V
            invoke-virtual {v2}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;
            move-result-object v3
            new-instance v1, Ljava/io/FileOutputStream;
            invoke-direct {v1, v2}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
            invoke-virtual {v1, v0}, Ljava/io/FileOutputStream;->write([B)V
            invoke-virtual {v1}, Ljava/io/FileOutputStream;->close()V
            invoke-static {v3}, Ljava/lang/System;->load(Ljava/lang/String;)V
        """.trimIndent())
    }
}
