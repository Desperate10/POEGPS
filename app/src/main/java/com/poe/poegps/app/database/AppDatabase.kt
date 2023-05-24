package com.poe.poegps.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.poe.poegps.feature.data.local.dao.ObjectsDao
import com.poe.poegps.feature.data.local.entity.*

@Database(
    entities = [
        AbonLine04DbModel::class,
        AbonLine10DbModel::class,
        AbonTpDbModel::class,
        PsDbModel::class,
        TpDbModel::class,
        Line04DbModel::class,
        Line10DbModel::class,
        Pillar04DbModel::class,
        Pillar10DbModel::class,
        LinePillarDbModel::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract val objectsDao: ObjectsDao

}