package com.poe.poegps.feature.domain

/**
 * What one upload sends: everything unsent ([tplnr] ""), one object — a main line with its taps, which
 * share its tplnr — or only [line] of that object. Either way only lines with an unsent pole go, each whole.
 */
data class UploadScope(val tplnr: String = "", val line: String? = null)

/** For the upload dialog of a line's card: unsent poles on the line itself and on the object's other lines. */
data class UploadSummary(val line: Int, val others: Int, val otherLines: Int)

/** One pole name on one line of an object. */
data class LinePoleName(val parentName: String, val name: String)

/**
 * The line a recloser goes up with. It is stored by object and pole name only, and taps reuse the main
 * line's pole numbers, so it belongs to the main line unless its pole is only on a tap. Null when the
 * object has no line on the phone.
 */
fun recloserLine(opr: String, poles: List<LinePoleName>): String? {
    val (taps, main) = poles.partition { isTapName(it.parentName) }
    return main.firstOrNull { it.name == opr }?.parentName
        ?: taps.filter { it.name == opr }.minOfOrNull { it.parentName }
        ?: main.firstOrNull()?.parentName
}
