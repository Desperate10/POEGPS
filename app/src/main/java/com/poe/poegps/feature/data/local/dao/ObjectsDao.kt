package com.poe.poegps.feature.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poe.poegps.feature.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine04(line: Line04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine10(line: Line10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonLine04(line: AbonLine04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonLine10(line: AbonLine10DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTp(tp: TpDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbonTp(tp: AbonTpDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPs(ps: PsDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar04(pillar: Pillar04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar10(pillar: Pillar10DbModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun copyPillarForOtp(pillar: LinePillarDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPillar(pillar: LinePillarDbModel): Long

    @Delete
    suspend fun deletePillar(pillar: LinePillarDbModel)

    @Query("SELECT * FROM tp")
    fun getTps(): Flow<List<TpDbModel>>

    @Query("SELECT * FROM line04 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getLines04(tplnr: String): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line10 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getLines10(tplnr: String): Flow<List<Line10DbModel>>

    @Query("SELECT * FROM abon_line04 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getAbonLines04(tplnr: String): Flow<List<AbonLine04DbModel>>

    @Query("SELECT * FROM abon_line10 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getAbonLines10(tplnr: String): Flow<List<AbonLine10DbModel>>

    @Query("SELECT * FROM line10 WHERE name LIKE 'ПЛ%'")
    fun getLineList10(): Flow<List<Line10DbModel>>

    @Query("SELECT * FROM line04 WHERE name LIKE 'ПЛ%'")
    fun getLineList04(): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line_pillar WHERE parentName LIKE :pltxt")
    fun getPillarsOfLine(pltxt: String): Flow<List<LinePillarDbModel>>

    @Query("SELECT * FROM pillars04 WHERE tplnr LIKE :tplnr")
    fun getPillars04ByTplnr(tplnr: String): Flow<List<Pillar04DbModel>>

    @Query("SELECT * FROM pillars10 WHERE tplnr LIKE :tplnr")
    fun getPillars10ByTplnr(tplnr: String): Flow<List<Pillar10DbModel>>

    @Query("SELECT * FROM tp WHERE tplnr LIKE '%-P'|| :tplnr || '%'")
    fun getTp(tplnr: String): Flow<List<TpDbModel>>

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

    @Query("DELETE FROM line04 WHERE name LIKE :pltxt")
    suspend fun deleteLine04(pltxt: String)

    @Query("DELETE FROM line10 WHERE name LIKE :pltxt")
    suspend fun deleteLine10(pltxt: String)

    @Query("DELETE FROM abon_line04 WHERE name LIKE :pltxt")
    suspend fun deleteAbonLine04(pltxt: String)

    @Query("DELETE FROM abon_line10 WHERE name LIKE :pltxt")
    suspend fun deleteAbonLine10(pltxt: String)

    @Query("DELETE FROM line_pillar WHERE parentName LIKE :pltxt")
    fun deleteSavedPillars(pltxt: String)

}