package com.poe.poegps.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.poe.poegps.feature.data.local.dao.ObjectsDao
import com.poe.poegps.feature.data.local.entity.*

@Database(entities = [PsDbModel::class, TpDbModel::class, Kl04DbModel::class, Line04DbModel::class, Kl10DbModel::class, Line10DbModel::class, PillarDbModel::class],
    version = 1,
    exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract val objectsDao: ObjectsDao

}