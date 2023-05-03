package com.poe.poegps.feature.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.poe.poegps.feature.data.local.entity.TpDbModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectsDao {

    @Query("SELECT * FROM tp")
    fun getTps(): Flow<List<TpDbModel>>
}