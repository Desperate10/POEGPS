package com.poe.poegps.feature.domain

/**
 * Splits the /save answer into reasons per line. The server lists refused objects only by tplnr, and
 * its message is "Не прийнято (перевірте координати): <line> оп. <pole>: <reason>; … Вивантажено успішно! <tplnr> <reason>; …".
 * A coordinate refusal names its line; a refusal after the save names only the object.
 */
object UploadReport {
    private const val REFUSED_PREFIX = "Не прийнято (перевірте координати):"
    private const val SAVED = "Вивантажено успішно!"

    data class Parsed(
        /** "<line> оп. <pole>: <reason>" parts: the line is known. */
        val poleRefusals: List<String>,
        /** "<tplnr> <reason>" parts: only the object is known. */
        val objectRefusals: List<String>,
    )

    fun parse(message: String): Parsed {
        val savedAt = message.indexOf(SAVED)
        val before = if (savedAt >= 0) message.substring(0, savedAt) else message
        val after = if (savedAt >= 0) message.substring(savedAt + SAVED.length) else ""
        fun parts(s: String) = s.split(";").map { it.trim().trimEnd('.') }.filter { it.isNotEmpty() }
        val refused = before.substringAfter(REFUSED_PREFIX, "")
        return Parsed(parts(refused), parts(after))
    }

    /**
     * Why the line [lineName] of object [tplnr] was refused ("" when the reason is unknown), or null when it
     * was accepted. A refused tplnr means some line of the object was refused: when the message names lines
     * or objects but not this line nor its object, it was another line, and this one was written.
     */
    fun reasonFor(lineName: String, tplnr: String, refusedTplnrs: Set<String>, parsed: Parsed): String? {
        if (tplnr !in refusedTplnrs) return null
        val own = parsed.poleRefusals.filter { it.startsWith("$lineName оп. ") }.map { it.removePrefix("$lineName ") }
        val forObject = parsed.objectRefusals.filter { it.startsWith("$tplnr ") }.map { it.removePrefix("$tplnr ") }
        return when {
            own.isNotEmpty() || forObject.isNotEmpty() -> (own + forObject).joinToString("; ")
            parsed.poleRefusals.isNotEmpty() || parsed.objectRefusals.isNotEmpty() -> null
            else -> ""
        }
    }
}
