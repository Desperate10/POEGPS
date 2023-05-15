package com.poe.poegps.feature.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poe.poegps.feature.data.local.entity.Line04DbModel
import com.poe.poegps.feature.data.local.entity.PillarDbModel
import com.poe.poegps.feature.data.local.entity.PsDbModel
import com.poe.poegps.feature.data.local.entity.TpDbModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: Line04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTp(tp: TpDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPs(ps: PsDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar(pillar: PillarDbModel)

    @Query("SELECT * FROM tp")
    fun getTps(): Flow<List<TpDbModel>>

    @Query("SELECT * FROM line04 WHERE tplnr LIKE 'PO-F-20-L' || :tplnr || '%' GROUP BY name")
    fun getLines(tplnr: String): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM tp WHERE tplnr LIKE 'PO-F-' || :filial ||'-P'|| :tplnr || '%'")
    fun getTp(filial: String, tplnr: String): Flow<List<TpDbModel>>
}