package com.poe.poegps.feature.data.local

import com.poe.poegps.app.database.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A branch download used to only add rows, so switching branch, or the server moving a line to another
 * branch (23 → 31 by SAP plant), left the old branch's objects on the phone. After a part of the download
 * succeeds, this removes the rows of its tables that the server didn't send.
 *
 * Objects with work not yet uploaded (unsent poles, TP coordinates, reclosers) stay whatever branch they
 * are in, so nothing captured is hidden before it reaches GIS. Captured rows themselves (line_pillar,
 * recloser) are never touched: an object that comes back with its branch shows its progress again.
 */
@Singleton
class BranchPruner @Inject constructor(private val db: AppDatabase) {

    /** For each table, keeps only the tplnrs the server just sent, plus objects with unsent work. */
    fun prune(keepByTable: Map<String, Collection<String>>) {
        require(TABLES.containsAll(keepByTable.keys)) { "not a downloaded table: ${keepByTable.keys - TABLES}" }
        db.runInTransaction {
            val sql = db.openHelper.writableDatabase
            sql.execSQL("CREATE TEMP TABLE IF NOT EXISTS keep_tplnr (tplnr TEXT PRIMARY KEY)")
            sql.compileStatement("INSERT OR IGNORE INTO keep_tplnr (tplnr) VALUES (?)").use { insert ->
                for ((table, keep) in keepByTable) {
                    sql.execSQL("DELETE FROM keep_tplnr")
                    for (tplnr in keep) {
                        insert.bindString(1, tplnr)
                        insert.executeInsert()
                        insert.clearBindings()
                    }
                    sql.execSQL(
                        "DELETE FROM $table WHERE tplnr NOT IN (SELECT tplnr FROM keep_tplnr)" +
                            " AND tplnr NOT IN (SELECT tplnr FROM line_pillar WHERE isSent = 0)" +
                            " AND tplnr NOT IN (SELECT tplnr FROM recloser WHERE isSent = 0)"
                    )
                }
            }
            sql.execSQL("DELETE FROM keep_tplnr")
        }
    }

    companion object {
        // the branch-specific reference tables a download fills (ps and wires are the same for every branch)
        val TABLES = setOf(
            "line04", "line10", "abon_line04", "abon_line10",
            "kl04", "kl10", "abon_kl04", "abon_kl10",
            "tp", "abon_tp", "pillars04", "pillars10",
        )
    }
}
