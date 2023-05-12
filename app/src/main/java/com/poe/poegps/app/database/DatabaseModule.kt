package com.poe.poegps.app.database

import android.content.Context
import androidx.room.Room
import com.poe.poegps.feature.data.local.dao.ObjectsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    fun provideObjectsDao(
        database: AppDatabase
    ): ObjectsDao {
        return database.objectsDao
    }
}