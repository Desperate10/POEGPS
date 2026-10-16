package com.poe.poegps.feature.data.local.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.poe.poegps.feature.data.local.entity.*
import com.poe.poegps.feature.domain.model.GisPoleCount
import com.poe.poegps.feature.domain.model.LineProgress
import com.poe.poegps.feature.domain.model.UploadLine
import com.poe.poegps.feature.domain.model.RecloserCount
import com.poe.poegps.feature.domain.model.UploadGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectsDao {

    suspend fun insertLine04IfNotExist(line: Line04DbModel) {
        if (getLine04(line.tplnr) == null) {
            insertLine04(line)
        }
    }
    @Query("SELECT * FROM line04 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getLine04(tplnr: String): Line04DbModel?

    suspend fun insertLine10IfNotExist(line: Line10DbModel) {
        if (getLine10(line.tplnr) == null) {
            insertLine10(line)
        }
    }

    @Query("SELECT * FROM line10 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getLine10(tplnr: String): Line10DbModel?

    suspend fun insertAbonLine04IfNotExist(line: AbonLine04DbModel) {
        if (getAbonLine04(line.tplnr) == null) {
            insertAbonLine04(line)
        }
    }
    @Query("SELECT * FROM abon_line04 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getAbonLine04(tplnr: String): AbonLine04DbModel?

    suspend fun insertAbonLine10IfNotExist(line: AbonLine10DbModel) {
        if (getAbonLine10(line.tplnr) == null) {
            insertAbonLine10(line)
        }
    }

    @Query("SELECT * FROM abon_line10 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getAbonLine10(tplnr: String): AbonLine10DbModel?

    suspend fun insertKl04IfNotExist(line: Kl04DbModel) {
        if (getKl04(line.tplnr) == null) {
            insertKl04(line)
        }
    }
    @Query("SELECT * FROM kl04 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getKl04(tplnr: String): Kl04DbModel?

    suspend fun insertKl10IfNotExist(line: Kl10DbModel) {
        if (getKl10(line.tplnr) == null) {
            insertKl10(line)
        }
    }
    @Query("SELECT * FROM kl10 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getKl10(tplnr: String): Kl10DbModel?

    suspend fun insertAbonKl04IfNotExist(line: AbonKl04DbModel) {
        if (getAbonKl04(line.tplnr) == null) {
            insertAbonKl04(line)
        }
    }
    @Query("SELECT * FROM abon_kl04 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getAbonKl04(tplnr: String): AbonKl04DbModel?

    suspend fun insertAbonKl10IfNotExist(line: AbonKl10DbModel) {
        if (getAbonKl10(line.tplnr) == null) {
            insertAbonKl10(line)
        }
    }
    @Query("SELECT * FROM abon_kl10 WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getAbonKl10(tplnr: String): AbonKl10DbModel?


    suspend fun insertTpIfNotExist(tp: TpDbModel) {
        if (getSingleTp(tp.tplnr) == null) {
            insertTp(tp)
        }
    }

    @Query("SELECT * FROM tp WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getSingleTp(tplnr: String): TpDbModel

    suspend fun insertAbonTpIfNotExist(tp: AbonTpDbModel) {
        if (getSingleAbonTp(tp.tplnr) == null) {
            insertAbonTp(tp)
        }
    }

    @Query("SELECT * FROM abon_tp WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getSingleAbonTp(tplnr: String): AbonTpDbModel

    suspend fun insertPsIfNotExist(ps: PsDbModel) {
        if (getSinglePs(ps.tplnr) == null) {
            insertPs(ps)
        }
    }

    @Query("SELECT * FROM ps WHERE tplnr = :tplnr LIMIT 1")
    suspend fun getSinglePs(tplnr: String): PsDbModel?

    suspend fun insertPillar04IfNotExist(pillar: Pillar04DbModel) {
        if (getSinglePillar04(pillar.tplnr, pillar.name) == null) {
            insertPillar04(pillar)
        }
    }

    @Query("SELECT * FROM pillars04 WHERE tplnr = :tplnr AND name = :name LIMIT 1")
    suspend fun getSinglePillar04(tplnr: String, name: String): Pillar04DbModel?

    suspend fun insertPillar10IfNotExist(pillar: Pillar10DbModel) {
        if (getSinglePillar10(pillar.tplnr, pillar.name) == null) {
            insertPillar10(pillar)
        }
    }

    @Query("SELECT * FROM pillars10 WHERE tplnr = :tplnr AND name = :name LIMIT 1")
    suspend fun getSinglePillar10(tplnr: String, name: String): Pillar10DbModel?

    // A branch has tens of thousands of poles and the tables have no index on (tplnr, name):
    // checking them one by one took minutes. Same rule, one read of the keys and one transaction.
    @Transaction
    suspend fun insertPillars04IfNotExist(pillars: List<Pillar04DbModel>) {
        val known = getPillar04Keys().toHashSet()
        insertPillars04(pillars.filter { known.add(PoleKey(it.tplnr, it.name)) })
    }

    @Transaction
    suspend fun insertPillars10IfNotExist(pillars: List<Pillar10DbModel>) {
        val known = getPillar10Keys().toHashSet()
        insertPillars10(pillars.filter { known.add(PoleKey(it.tplnr, it.name)) })
    }

    @Query("SELECT tplnr, name FROM pillars04")
    suspend fun getPillar04Keys(): List<PoleKey>

    @Query("SELECT tplnr, name FROM pillars10")
    suspend fun getPillar10Keys(): List<PoleKey>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillars04(pillars: List<Pillar04DbModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillars10(pillars: List<Pillar10DbModel>)

    suspend fun insertWire04IfNotExist(wire: Wire04DbModel) {
        if (getSingleWire04(wire.name) == null) {
            insertWire04(wire)
        }
    }

    @Query("SELECT * FROM wire04 WHERE name = :name LIMIT 1")
    suspend fun getSingleWire04(name: String): Wire04DbModel?

    suspend fun insertWire10IfNotExist(wire: Wire10DbModel) {
        if (getSingleWire10(wire.name) == null) {
            insertWire10(wire)
        }
    }

    @Query("SELECT * FROM wire10 WHERE name = :name LIMIT 1")
    suspend fun getSingleWire10(name: String): Wire10DbModel?

    /*suspend fun insertRecloserIfNotExist(recloser: RecloserDbModel?) {
        if (getSingleRecloser(recloser?.name) == null) {
            insertRecloser(recloser)
        }
    }

    @Query("SELECT * FROM recloser WHERE name = :name LIMIT 1")
    suspend fun getSingleRecloser(name: String?): RecloserDbModel?*/

    @Query("SELECT * FROM line_pillar WHERE tplnr = :tplnr LIMIT 1")
    suspend fun tpAlreadySaved(tplnr: String): LinePillarDbModel?

    suspend fun saveNewTpCoord(tp: LinePillarDbModel) {
        if (tpAlreadySaved(tp.tplnr) == null) {
            // a new row: tp.id is the TP table's id, and inserting it here (REPLACE) would
            // overwrite whichever captured pole happens to have the same line_pillar id
            insertSavedPillar(tp.copy(id = 0, sortOrder = nextSortOrder()))
        } else {
            updateSavedPillar(tp.lat, tp.lng, tp.tplnr)
        }
        if (tp.isAbon) {
            updateAbonTpCoords(tp.lat, tp.lng, tp.tplnr)
        } else {
            updateTpCoords(tp.lat, tp.lng, tp.tplnr)
        }
    }

    @Query("UPDATE tp SET lat = :lat, lng = :lng  WHERE tplnr = :tplnr")
    suspend fun updateTpCoords(lat: String, lng: String, tplnr: String)

    @Query("UPDATE abon_tp SET lat = :lat, lng = :lng  WHERE tplnr = :tplnr")
    suspend fun updateAbonTpCoords(lat: String, lng: String, tplnr: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWire04(wire: Wire04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWire10(wire: Wire10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine04(line: Line04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine10(line: Line10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonLine04(line: AbonLine04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonLine10(line: AbonLine10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKl04(line: Kl04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKl10(line: Kl10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonKl04(line: AbonKl04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonKl10(line: AbonKl10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTp(tp: TpDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonTp(tp: AbonTpDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPs(ps: PsDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecloser(recloser: RecloserDbModel) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar04(pillar: Pillar04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar10(pillar: Pillar10DbModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPillarIgnoring(pillar: LinePillarDbModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPillarsIgnoring(pillars: List<LinePillarDbModel>)

    suspend fun copyPillarForOtp(pillar: LinePillarDbModel) {
        insertPillarIgnoring(pillar.copy(sortOrder = nextSortOrder()))
    }

    /** Appends to the line, or goes right after [afterId] when given. 0 = the line already has this name. */
    @Transaction
    suspend fun insertSavedPillarIfNotExist(pillar: LinePillarDbModel, afterId: Int? = null): Long {
        if (getSinglePillar(pillar.tplnr, pillar.parentName, pillar.name) != null) return 0L
        val order = afterId?.let { orderAfter(it) } ?: nextSortOrder()
        return insertSavedPillar(pillar.copy(sortOrder = order))
    }

    // halfway to the next pole of the same line, or after everything when it is the last one
    private suspend fun orderAfter(id: Int): Double {
        val after = getPillarById(id) ?: return nextSortOrder()
        val next = getNextInLine(after.tplnr, after.parentName, after.sortOrder, after.id) ?: return nextSortOrder()
        return (after.sortOrder + next.sortOrder) / 2
    }

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM line_pillar")
    suspend fun nextSortOrder(): Double

    @Query("SELECT * FROM line_pillar WHERE id = :id")
    suspend fun getPillarById(id: Int): LinePillarDbModel?

    @Query(
        "SELECT * FROM line_pillar WHERE tplnr = :tplnr AND parentName = :parentName " +
            "AND (sortOrder > :order OR (sortOrder = :order AND id > :id)) ORDER BY sortOrder, id LIMIT 1"
    )
    suspend fun getNextInLine(tplnr: String, parentName: String, order: Double, id: Int): LinePillarDbModel?

    @Query("SELECT COUNT(*) FROM line_pillar WHERE tplnr = :tplnr AND parentName = :parentName")
    suspend fun countLinePoles(tplnr: String, parentName: String): Int

    /** Loads a GIS line's poles in the given order, only into a line that has none on the phone. */
    @Transaction
    suspend fun importLine(tplnr: String, parentName: String, poles: List<LinePillarDbModel>): Int {
        if (poles.isEmpty() || countLinePoles(tplnr, parentName) > 0) return 0
        val base = nextSortOrder()
        insertPillarsIgnoring(poles.mapIndexed { i, p -> p.copy(id = 0, sortOrder = base + i) })
        return poles.size
    }

    @Query("SELECT id FROM line_pillar WHERE tplnr = :tplnr AND parentName = :parentName")
    suspend fun getLinePoleIds(tplnr: String, parentName: String): List<Int>

    @Query("UPDATE line_pillar SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Int, order: Double)

    /**
     * Completes a line surveyed before its GIS poles were loaded: [merged] is the whole line in its new
     * order, the phone's poles by their id, GIS poles to add with id 0. Nothing changes (0) when the
     * line's poles on the phone are no longer the ones the merge was planned on.
     */
    @Transaction
    suspend fun mergeLine(tplnr: String, parentName: String, merged: List<LinePillarDbModel>): Int {
        val planned = merged.filter { it.id != 0 }.map { it.id }.toSet()
        if (getLinePoleIds(tplnr, parentName).toSet() != planned) return 0
        val base = nextSortOrder()
        val added = merged.mapIndexedNotNull { i, p ->
            if (p.id != 0) {
                setSortOrder(p.id, base + i)
                null
            } else {
                p.copy(sortOrder = base + i)
            }
        }
        insertPillarsIgnoring(added)
        return added.size
    }

    // per line: a tap shares its main line's tplnr and may have a pole with the same name
    @Query("SELECT * FROM line_pillar WHERE tplnr = :tplnr AND parentName = :parentName AND name = :name LIMIT 1")
    suspend fun getSinglePillar(tplnr: String, parentName: String, name: String): LinePillarDbModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPillar(pillar: LinePillarDbModel) : Long

    @Query("UPDATE line_pillar SET lat = :lat, lng = :lng WHERE tplnr = :tplnr")
    suspend fun updateSavedPillar(lat: String, lng: String, tplnr: String)

    // a re-surveyed pole is no longer "as in GIS"
    @Query("UPDATE line_pillar SET lat = :lat, lng = :lng, isSent = 0, fromGis = 0 WHERE id = :id")
    suspend fun updatePillarCoords(id: Int, lat: String, lng: String)

    // copies of one pole in other lines: same object, same name, and the very coordinates text it had
    @Query(
        "UPDATE line_pillar SET lat = :lat, lng = :lng, isSent = 0, fromGis = 0 " +
            "WHERE tplnr = :tplnr AND name = :name AND lat = :oldLat AND lng = :oldLng AND id <> :id"
    )
    suspend fun moveCopies(id: Int, tplnr: String, name: String, oldLat: String, oldLng: String, lat: String, lng: String): Int

    /**
     * A pole captured or moved, with its copies: a tap starts with a copy of the pole it leaves from, and
     * a copy left at the old place would put the pole back there in GIS when both lines are uploaded.
     * Taps reuse pole numbers, so a copy is told by its unchanged coordinates, never by name alone;
     * a pole that had none has no copy to tell apart.
     */
    /**
     * A moved GIS pole put back where GIS has it ([lat]/[lng] as GIS gives them), copies in taps too.
     * It is left unsent: if the move was uploaded meanwhile, GIS has the moved point and the next
     * upload takes it back; if not, the pole is sent as GIS has it and nothing moves.
     */
    @Transaction
    suspend fun restoreGisPosition(id: Int, lat: String, lng: String): Int {
        val pole = getPillarById(id) ?: return 0
        markAsInGis(id, lat, lng)
        if (pole.lat.toDoubleOrNull()?.let { it != 0.0 } != true) return 0
        return moveCopies(id, pole.tplnr, pole.name, pole.lat, pole.lng, lat, lng)
    }

    @Query("UPDATE line_pillar SET lat = :lat, lng = :lng, isSent = 0, fromGis = 1 WHERE id = :id")
    suspend fun markAsInGis(id: Int, lat: String, lng: String)

    @Transaction
    suspend fun movePole(id: Int, lat: String, lng: String): Int {
        val pole = getPillarById(id) ?: return 0
        updatePillarCoords(id, lat, lng)
        if (pole.lat.toDoubleOrNull()?.let { it != 0.0 } != true) return 0
        return moveCopies(id, pole.tplnr, pole.name, pole.lat, pole.lng, lat, lng)
    }

    @Delete
    suspend fun deletePillar(pillar: LinePillarDbModel)

    @Query("DELETE FROM recloser WHERE tplnr = :tplnr AND name = :name")
    suspend fun deleteRecloser(tplnr: String, name: String)

    // the server rebuilds each line from the poles it receives, so a line with any unsent pole
    // is sent whole (sent poles included), in editor order
    @Query(
        "SELECT * FROM line_pillar p WHERE p.lat <> '0.0' AND EXISTS (" +
            "SELECT 1 FROM line_pillar u WHERE u.lat <> '0.0' AND u.isSent = 0 " +
            "AND u.tplnr = p.tplnr AND u.parentName = p.parentName " +
            "AND u.category = p.category AND u.isAbon = p.isAbon) " +
            "ORDER BY p.sortOrder, p.id"
    )
    suspend fun getSavedPillars(): List<LinePillarDbModel>

    @Query("SELECT * FROM line_pillar WHERE lat <> '0.0' AND tplnr LIKE :tplnr ORDER BY sortOrder, id")
    suspend fun getSavedPillarsByTplnr(tplnr: String): List<LinePillarDbModel>

    @Query(
        "SELECT tplnr, parentName, isAbon, COUNT(*) AS total, " +
            // poles loaded from GIS count as not surveyed yet
            "SUM(CASE WHEN lat <> '0.0' AND fromGis = 0 THEN 1 ELSE 0 END) AS captured, " +
            "SUM(CASE WHEN lat <> '0.0' AND isSent = 0 THEN 1 ELSE 0 END) AS unsent " +
            "FROM line_pillar GROUP BY tplnr, parentName, isAbon"
    )
    fun getLineProgress(): Flow<List<LineProgress>>

    @Query(
        "SELECT (SELECT COUNT(*) FROM line_pillar WHERE lat <> '0.0' AND isSent = 0) + " +
            "(SELECT COUNT(*) FROM recloser WHERE lat <> '0.0' AND isSent = 0)"
    )
    fun countUnsent(): Flow<Int>

    // what an upload of everything reports: the unsent poles, per line
    @Query(
        "SELECT tplnr, COALESCE(NULLIF(parentName, ''), MIN(name)) AS name, COUNT(*) AS poles FROM line_pillar " +
            "WHERE lat <> '0.0' AND isSent = 0 GROUP BY tplnr, parentName ORDER BY MIN(id)"
    )
    suspend fun getUnsentGroups(): List<UploadGroup>

    @Query(
        "SELECT tplnr, COALESCE(NULLIF(parentName, ''), MIN(name)) AS name, COUNT(*) AS poles FROM line_pillar " +
            "WHERE lat <> '0.0' AND tplnr LIKE :tplnr GROUP BY tplnr, parentName ORDER BY MIN(id)"
    )
    suspend fun getGroupsByTplnr(tplnr: String): List<UploadGroup>

    // the lines an upload sends, picked like getSavedPillars (all) and getSavedPillarsByTplnr (one object)
    @Query(
        "SELECT tplnr, parentName AS name, MIN(pillarType) AS pillarType, category, isAbon, COUNT(*) AS poles " +
            "FROM line_pillar p WHERE lat <> '0.0' AND parentName <> '' AND (" +
            "(:tplnr <> '' AND tplnr LIKE :tplnr) OR (:tplnr = '' AND EXISTS (" +
            "SELECT 1 FROM line_pillar u WHERE u.lat <> '0.0' AND u.isSent = 0 " +
            "AND u.tplnr = p.tplnr AND u.parentName = p.parentName " +
            "AND u.category = p.category AND u.isAbon = p.isAbon))) " +
            "GROUP BY tplnr, parentName, category, isAbon"
    )
    suspend fun getLinesToUpload(tplnr: String): List<UploadLine>

    @Query("SELECT tplnr, COUNT(*) AS count FROM recloser WHERE lat <> '0.0' AND isSent = 0 GROUP BY tplnr")
    suspend fun getUnsentRecloserCounts(): List<RecloserCount>

    @Query("SELECT * FROM recloser WHERE lat <> '0.0' AND isSent = 0")
    suspend fun getSavedReclosers(): List<RecloserDbModel>

    @Query("SELECT * FROM recloser WHERE tplnr LIKE '%' || :tplnr || '%'")
    suspend fun getSavedReclosersByTplnr(tplnr: String): List<RecloserDbModel>

    @Query("SELECT * FROM tp WHERE lat <> '0.0' ORDER BY pltxt ASC")
    fun getTps(): Flow<List<TpDbModel>>

    @Query("SELECT * FROM abon_tp WHERE lat <> '0.0' ORDER BY pltxt ASC")
    fun getAbonTps(): Flow<List<TpDbModel>>

    @Query("SELECT * FROM line04 WHERE tplnr LIKE '%-L' || :query || '%' " +
            "OR LOWER(name) LIKE LOWER('%' || :query || '%')" +
            "GROUP BY name")
    fun getLines04(query: String): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line10 WHERE tplnr LIKE '%-L' || :query || '%'" +
            "OR LOWER(name) LIKE LOWER('%' || :query || '%')" +
            "GROUP BY name")
    fun getLines10(query: String): Flow<List<Line10DbModel>>

    @Query("SELECT * FROM abon_line04 WHERE tplnr LIKE '%-L' || :query || '%' " +
            "OR LOWER(name) LIKE LOWER('%' || :query || '%')" +
            "GROUP BY name")
    fun getAbonLines04(query: String): Flow<List<AbonLine04DbModel>>

    @Query("SELECT * FROM abon_line10 WHERE tplnr LIKE '%-L' || :query || '%' " +
            "OR LOWER(name) LIKE LOWER('%' || :query || '%')" +
            "GROUP BY name")
    fun getAbonLines10(query: String): Flow<List<AbonLine10DbModel>>

    @Query("SELECT * FROM kl04 WHERE tplnr LIKE '%-L' || :query || '%' " +
            "OR LOWER(name) LIKE LOWER('%' || :query || '%')" +
            "GROUP BY name")
    fun getKls04(query: String): Flow<List<Kl04DbModel>>

    @Query("SELECT * FROM kl10 WHERE tplnr LIKE '%-L' || :query || '%' " +
            "OR LOWER(name) LIKE LOWER('%' || :query || '%')" +
            "GROUP BY name")
    fun getKls10(query: String): Flow<List<Kl10DbModel>>

    @Query("SELECT * FROM abon_kl04 WHERE tplnr LIKE '%-L' || :query || '%' " +
            "OR LOWER(name) LIKE LOWER('%' || :query || '%')" +
            "GROUP BY name")
    fun getAbonKls04(query: String): Flow<List<AbonKl04DbModel>>

    @Query("SELECT * FROM abon_kl10 WHERE tplnr LIKE '%-L' || :query || '%' " +
            "OR LOWER(name) LIKE LOWER('%' || :query || '%')" +
            "GROUP BY name")
    fun getAbonKls10(query: String): Flow<List<AbonKl10DbModel>>

    @Query("SELECT * FROM line10 WHERE name LIKE 'ПЛ%'")
    fun getLineList10(): Flow<List<Line10DbModel>>

    @Query("SELECT * FROM line04 WHERE name LIKE 'ПЛ%'")
    fun getLineList04(): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line_pillar WHERE parentName LIKE :pltxt ORDER BY sortOrder, id")
    fun getPillarsOfLine(pltxt: String): Flow<List<LinePillarDbModel>>

    @Query("SELECT * FROM pillars04 WHERE tplnr LIKE :tplnr")
    fun getPillars04ByTplnr(tplnr: String): Flow<List<Pillar04DbModel>>

    @Query("SELECT * FROM pillars10 WHERE tplnr LIKE :tplnr")
    fun getPillars10ByTplnr(tplnr: String): Flow<List<Pillar10DbModel>>

    // GIS poles per line for the object cards, credited to the line's first row like B9 below
    @Query("SELECT p.tplnr AS tplnr, " +
            "(SELECT l.name FROM line04 l WHERE l.tplnr = p.tplnr ORDER BY l.id LIMIT 1) AS name, " +
            "COUNT(*) AS poles FROM pillars04 p GROUP BY p.tplnr")
    fun getGisPoleCounts04(): Flow<List<GisPoleCount>>

    @Query("SELECT p.tplnr AS tplnr, " +
            "(SELECT l.name FROM line10 l WHERE l.tplnr = p.tplnr ORDER BY l.id LIMIT 1) AS name, " +
            "COUNT(*) AS poles FROM pillars10 p GROUP BY p.tplnr")
    fun getGisPoleCounts10(): Flow<List<GisPoleCount>>

    // B9: GIS poles inside a lat/lng box, each with its line's name (taps share the line's tplnr, so the first one)
    @Query("SELECT p.id, p.tplnr, p.name, p.wire, p.lat, p.lng, " +
            "(SELECT l.name FROM line04 l WHERE l.tplnr = p.tplnr ORDER BY l.id LIMIT 1) AS lineName " +
            "FROM pillars04 p WHERE CAST(p.lat AS REAL) BETWEEN :minLat AND :maxLat " +
            "AND CAST(p.lng AS REAL) BETWEEN :minLng AND :maxLng LIMIT 500")
    suspend fun getPillars04In(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<PillarOnLine>

    @Query("SELECT p.id, p.tplnr, p.name, p.wire, p.lat, p.lng, " +
            "(SELECT l.name FROM line10 l WHERE l.tplnr = p.tplnr ORDER BY l.id LIMIT 1) AS lineName " +
            "FROM pillars10 p WHERE CAST(p.lat AS REAL) BETWEEN :minLat AND :maxLat " +
            "AND CAST(p.lng AS REAL) BETWEEN :minLng AND :maxLng LIMIT 500")
    suspend fun getPillars10In(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<PillarOnLine>

    @Query("SELECT * FROM tp WHERE tplnr LIKE '%-P'|| :query || '%' " +
    "OR LOWER(pltxt) LIKE LOWER('%' || :query || '%')")
    fun getTpFlow(query: String): Flow<List<TpDbModel>>

    @Query("SELECT * FROM abon_tp WHERE tplnr LIKE '%-P'|| :query || '%' " +
            "OR LOWER(pltxt) LIKE LOWER('%' || :query || '%')")
    fun getAbonTpFlow(query: String): Flow<List<AbonTpDbModel>>

    @Query("SELECT * FROM ps WHERE tplnr LIKE '%-P'|| :tplnr || '%'")
    fun getPs(tplnr: String): Flow<List<PsDbModel>>

    @Query("SELECT * FROM wire04")
    fun getWire04(): Flow<List<Wire04DbModel>>

    @Query("SELECT * FROM wire10")
    fun getWire10(): Flow<List<Wire10DbModel>>

    @Query("SELECT name FROM line04 WHERE tplnr LIKE :tplnr")
    suspend fun getParentName(tplnr: String): String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtpaika04(otpaika: Line04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtpaika10(otpaika: Line10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonOtpaika04(otpaika: AbonLine04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonOtpaika10(otpaika: AbonLine10DbModel)

    @Query("UPDATE tp SET lng = :lng, lat = :lat WHERE tplnr LIKE :tplnr")
    suspend fun updateTpCoordinates(tplnr: String, lng: String, lat: String)

    @Query("UPDATE abon_tp SET lng = :lng, lat = :lat WHERE tplnr LIKE :tplnr")
    suspend fun updateAbonTpCoordinates(tplnr: String, lng: String, lat: String)

    @Query("UPDATE ps SET lng = :lng, lat = :lat WHERE tplnr LIKE :tplnr")
    suspend fun updatePsCoordinates(tplnr: String, lng: String, lat: String)

    @Query("DELETE FROM line04 WHERE name LIKE :pltxt")
    suspend fun deleteLine04(pltxt: String)

    @Query("DELETE FROM line10 WHERE name LIKE :pltxt")
    suspend fun deleteLine10(pltxt: String)

    @Query("DELETE FROM abon_line04 WHERE name LIKE :pltxt")
    suspend fun deleteAbonLine04(pltxt: String)

    @Query("DELETE FROM abon_line10 WHERE name LIKE :pltxt")
    suspend fun deleteAbonLine10(pltxt: String)

    @Query("DELETE FROM line_pillar WHERE parentName LIKE :pltxt")
    suspend fun deleteSavedPillars(pltxt: String)

    // ids is chunked by the caller to stay under SQLite's 999 bind-variable limit
    @Query("UPDATE line_pillar SET isSent = 1 WHERE id IN (:ids) AND tplnr NOT IN (:failedTplnrList)")
    suspend fun markPillarsSent(ids: List<Int>, failedTplnrList: List<String>)

    @Query("UPDATE recloser SET isSent = 1 WHERE id IN (:ids) AND tplnr NOT IN (:failedTplnrList)")
    suspend fun markReclosersSent(ids: List<Int>, failedTplnrList: List<String>)

    @Query("UPDATE line_pillar SET recloser = :name WHERE id = :id")
    suspend fun updateRecloser(id: Int, name: String)

}