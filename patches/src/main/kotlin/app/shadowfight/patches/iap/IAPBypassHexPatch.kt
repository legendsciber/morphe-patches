package app.shadowfight.patches.iap

import app.morphe.patcher.patch.rawResourcePatch
import java.util.logging.Logger

private fun findPattern(haystack: ByteArray, needle: ByteArray): Int {
    if (needle.isEmpty() || haystack.size < needle.size) return -1
    val lastStart = haystack.size - needle.size
    for (i in 0..lastStart) {
        var found = true
        for (j in needle.indices) {
            if (haystack[i + j] != needle[j]) {
                found = false
                break
            }
        }
        if (found) return i
    }
    return -1
}

@Suppress("unused")
val sfIAPBypassHexPatch = rawResourcePatch(
    name = "Shadow Fight 2 IAP Bypass (Hex)",
    description = "Patches libil2cpp.so to bypass server-side receipt verification and force items delivery.",
    default = true,
) {
    execute {
        val logger = Logger.getLogger("SF2HexPatch")
        val libPath = try {
            val f = get("lib/arm64-v8a/libil2cpp.so")
            if (f != null && f.exists()) "lib/arm64-v8a/libil2cpp.so" else null
        } catch (e: Exception) { null }

        if (libPath == null) {
            logger.info("IL2CPP hex patch: no libil2cpp.so found, skipping")
            return@execute
        }
        logger.info("SF2 IL2CPP hex patch: found $libPath")

        try {
            val libFile = get(libPath)
            val libBytes = libFile.readBytes()

            // ARM64 instruction patterns
            val ret = byteArrayOf(
                0xC0.toByte(), 0x03.toByte(), 0x5F.toByte(), 0xD6.toByte()
            )
            val retWithNop = byteArrayOf(
                0xC0.toByte(), 0x03.toByte(), 0x5F.toByte(), 0xD6.toByte(),
                0x1F.toByte(), 0x20.toByte(), 0x03.toByte(), 0xD5.toByte()
            )
            val movW0x0Ret = byteArrayOf(  // return PurchaseProcessingResult.Complete (0)
                0x00.toByte(), 0x00.toByte(), 0x80.toByte(), 0x52.toByte(),
                0xC0.toByte(), 0x03.toByte(), 0x5F.toByte(), 0xD6.toByte()
            )
            val movW0x1Ret = byteArrayOf(  // return true
                0x20.toByte(), 0x00.toByte(), 0x80.toByte(), 0x52.toByte(),
                0xC0.toByte(), 0x03.toByte(), 0x5F.toByte(), 0xD6.toByte()
            )

            // Hex patches targeting SF2 v2.46.0 libil2cpp.so
            // Identified via Il2CppDumper decompilation of global-metadata.dat + libil2cpp.so
            val hexPatches = listOf(
                // Patch 1: ProcessPurchase -> return Complete (0)
                // DGGBFKNKEAH.ProcessPurchase (IDetailedStoreListener callback)
                // Returns PurchaseProcessingResult. Returning 0 = Complete = purchase handled.
                Triple(
                    byteArrayOf(
                        0x28.toByte(), 0x00.toByte(), 0x80.toByte(), 0x52.toByte(),
                        0xC8.toByte(), 0x46.toByte(), 0x18.toByte(), 0x39.toByte(),
                        0xE0.toByte(), 0x03.toByte(), 0x14.toByte(), 0xAA.toByte(),
                        0xE1.toByte(), 0x03.toByte(), 0x1F.toByte(), 0xAA.toByte()
                    ),
                    movW0x0Ret,
                    "ProcessPurchase -> return Complete (skip server verification)"
                ),

                // Patch 2: DCGFJIGIDKH -> return true
                // FEFHGAHGKBK.DCGFJIGIDKH (IsProductUnlocked equivalent)
                // Returns bool. Making it return true = all products appear purchased.
                Triple(
                    byteArrayOf(
                        0xE0.toByte(), 0x03.toByte(), 0x14.toByte(), 0xAA.toByte(),
                        0xE1.toByte(), 0x03.toByte(), 0x1F.toByte(), 0xAA.toByte(),
                        0x6E.toByte(), 0x04.toByte(), 0xB6.toByte(), 0x97.toByte(),
                        0xE0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x36.toByte()
                    ),
                    movW0x1Ret,
                    "DCGFJIGIDKH (IsProductUnlocked) -> return true"
                ),

                // Patch 3: MMDPFIHOBJP -> ret
                // FEFHGAHGKBK.LGOBIOGNECC.MMDPFIHOBJP (HTTP response callback)
                // Receives (bool result, string data, object userData) from server verification.
                // Making it return immediately = ignore server response = no error popup.
                Triple(
                    byteArrayOf(
                        0x20.toByte(), 0x8E.toByte(), 0x00.toByte(), 0xB0.toByte(),
                        0x00.toByte(), 0x9C.toByte(), 0x44.toByte(), 0xF9.toByte(),
                        0x0A.toByte(), 0x12.toByte(), 0xAD.toByte(), 0x97.toByte(),
                        0x20.toByte(), 0x8E.toByte(), 0x00.toByte(), 0xB0.toByte()
                    ),
                    retWithNop,
                    "MMDPFIHOBJP (server response callback) -> ret (ignore response)"
                ),

                // Patch 4: CAGEJJOAJLJ -> ret
                // FEFHGAHGKBK.CAGEJJOAJLJ (process result handler)
                // Takes (bool success, string data, int statusCode).
                // Skip processing server response entirely.
                Triple(
                    byteArrayOf(
                        0xF4.toByte(), 0xA7.toByte(), 0x00.toByte(), 0xF0.toByte(),
                        0x88.toByte(), 0xEA.toByte(), 0x56.toByte(), 0x39.toByte(),
                        0xF3.toByte(), 0x03.toByte(), 0x00.toByte(), 0xAA.toByte(),
                        0x08.toByte(), 0x03.toByte(), 0x00.toByte(), 0x37.toByte()
                    ),
                    retWithNop,
                    "CAGEJJOAJLJ (result processor) -> ret (skip server result)"
                )
            )

            var patchedCount = 0
            for ((pattern, replacement, description) in hexPatches) {
                val idx = findPattern(libBytes, pattern)
                if (idx >= 0) {
                    for (i in replacement.indices) {
                        libBytes[idx + i] = replacement[i]
                    }
                    patchedCount++
                    logger.info("  PATCHED: $description (offset 0x${Integer.toHexString(idx)})")
                } else {
                    logger.info("  SKIPPED: $description (pattern not found)")
                }
            }

            if (patchedCount > 0) {
                libFile.writeBytes(libBytes)
                logger.info("SF2 IL2CPP hex patch COMPLETE: $patchedCount/${hexPatches.size} patches applied")
            } else {
                logger.info("SF2 IL2CPP hex patch: no known patterns matched - game version may have changed")
            }
        } catch (e: Exception) {
            logger.info("SF2 IL2CPP hex patch FAILED: ${e.message}")
            e.printStackTrace()
        }
    }
}
