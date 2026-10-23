package com.poe.poegps.feature.domain

/** A pole of the object on the phone, as the branch tree needs it; [lat]/[lng] are the stored texts. */
data class BranchPole(
    val line: String,
    val name: String,
    val lat: String,
    val lng: String,
    /** Surveyed on site (not as GIS has it). */
    val checked: Boolean,
    val unsent: Boolean,
)

/** One line of the object in the editor's branch list: the main line, or a tap [depth] levels below it. */
data class Branch(
    val line: String,
    val depth: Int,
    /** What the crew reads: the tap's real first and last poles; GIS names often say other numbers. */
    val label: String,
    val poles: Int,
    val checked: Int,
    val unsent: Int,
    val isMain: Boolean,
)

/** A tap by its poles on the phone: "від оп. 31 → 32", or "від оп. 31" when it has one. */
fun tapLabel(first: String?, last: String?): String = when {
    first == null -> "без опор"
    last == null || last == first -> "від оп. $first"
    else -> "від оп. $first → $last"
}

/**
 * The object's lines as a tree: [mainLine] first, then each tap under the line it hangs from, in the
 * order of its junction along that line. A tap hangs from the line holding its first pole: the same
 * pole, name and coordinates, as a tap's junction is a copy of it. Taps that hang on nothing follow
 * the main line at the top level.
 */
fun branchTree(mainLine: String, poles: List<BranchPole>): List<Branch> {
    val byLine = poles.groupBy { it.line }
    fun branch(line: String, depth: Int): Branch {
        val ps = byLine[line].orEmpty()
        return Branch(
            line = line, depth = depth,
            label = if (line == mainLine) "Магістраль" else tapLabel(ps.firstOrNull()?.name, ps.lastOrNull()?.name),
            poles = ps.size, checked = ps.count { it.checked }, unsent = ps.count { it.unsent },
            isMain = line == mainLine,
        )
    }
    val taps = byLine.keys.filter { it != mainLine }
    // the parent and the junction's place on it, for each tap that hangs somewhere
    val parentOf = taps.mapNotNull { tap ->
        val start = byLine.getValue(tap).first()
        val candidates = byLine.filterKeys { it != tap }.mapNotNull { (line, ps) ->
            val i = ps.indexOfFirst { it.name == start.name && it.lat == start.lat && it.lng == start.lng }
            if (i < 0) null else Triple(line, i, line == mainLine)
        }
        // the main line first: a junction copied into other taps is still on it
        candidates.sortedWith(compareBy({ !it.third }, { it.second })).firstOrNull()?.let { tap to (it.first to it.second) }
    }.toMap()
    val children = parentOf.entries.groupBy({ it.value.first }) { it.key to it.value.second }
        .mapValues { (_, list) -> list.sortedWith(compareBy({ it.second }, { it.first })).map { it.first } }

    val out = mutableListOf<Branch>()
    val seen = mutableSetOf<String>()
    fun walk(line: String, depth: Int) {
        if (!seen.add(line)) return
        out += branch(line, depth)
        children[line].orEmpty().forEach { walk(it, depth + 1) }
    }
    walk(mainLine, 0)
    // hanging on nothing on the phone, or in a loop of copies: still listed
    taps.filter { it !in seen }.sorted().forEach { walk(it, 0) }
    return out
}
