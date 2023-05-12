package com.poe.poegps.feature.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poe.poegps.feature.data.local.entity.Line04DbModel
import com.poe.poegps.feature.data.local.entity.TpDbModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: Line04DbModel)

    @Query("SELECT * FROM tp")
    fun getTps(): Flow<List<TpDbModel>>

    @Query("SELECT * FROM line04 WHERE tplnr LIKE '%' || :tplnr || '%'")
    fun getLines(tplnr: String): Flow<List<Line04DbModel>>
}