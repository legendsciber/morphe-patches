package app.deathrover.patches.shared

private val RET_PADDING = byteArrayOf(0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte())

/** A single binary edit: [replacement] is written at the end of [anchor], which is left intact. */
class RawPatchSite(
    val label: String,
    val anchor: ByteArray,
    val replacement: ByteArray,
    val requireRetPadding: Boolean = false,
)

/**
 * Returns the one offset at which [anchor] occurs in [bytes], and fails if it is absent or ambiguous.
 *
 * The scan advances a single byte at a time, so occurrences are counted *with* overlap. That is what
 * makes a short anchor safe: an anchor whose bytes nest inside itself would look unique to a stepwise
 * search yet really have several candidate positions, and only one of them is the one being patched.
 */
fun indexOfUnique(bytes: ByteArray, anchor: ByteArray, label: String): Int {
    var count = 0
    var at = -1
    var i = 0
    while (i <= bytes.size - anchor.size) {
        var match = true
        var j = 0
        while (j < anchor.size) {
            if (bytes[i + j] != anchor[j]) {
                match = false
                break
            }
            j++
        }
        if (match) {
            count++
            at = i
            if (count > 1) break
        }
        i++
    }
    require(count == 1) { "$label site not unique: $count matches" }
    return at
}

/**
 * Applies every site to [bytes] in order, locating each anchor against whatever the previous sites
 * already wrote. Every site is located on its own, so no site depends on another one's offset.
 */
fun applySites(bytes: ByteArray, sites: List<RawPatchSite>) {
    for (site in sites) {
        val at = indexOfUnique(bytes, site.anchor, site.label) + site.anchor.size
        require(at + site.replacement.size <= bytes.size) { "${site.label} writes out of bounds" }
        if (site.requireRetPadding) {
            for (k in site.replacement.indices) {
                require(bytes[at + k] == RET_PADDING[k % 4]) {
                    "code cave at ${at + k} is not padding (${site.label})"
                }
            }
        }
        site.replacement.copyInto(bytes, at)
    }
}
