package com.poe.poegps.app.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.poe.poegps.feature.data.local.dao.ObjectsDao
import com.poe.poegps.feature.data.remote.utils.FilialManager
import com.poe.poegps.feature.data.remote.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "data_store")

    @Singleton
    @Provides
    fun provideTokenManager(@ApplicationContext context: Context) : TokenManager {
        return TokenManager(context)
    }

    @Singleton
    @Provides
    fun provideFilialManager(@ApplicationContext context: Context) : FilialManager {
        return FilialManager(context)
    }

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideObjectsDao(
        database: AppDatabase
    ): ObjectsDao {
        return database.objectsDao
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `recloser` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tplnr` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `opr` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `lng` TEXT NOT NULL,
                    `lat` TEXT NOT NULL,
                    `isSent` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            database.execSQL("""
                    CREATE TABLE IF NOT EXISTS kl04 (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tplnr TEXT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
            database.execSQL("""
                    CREATE TABLE IF NOT EXISTS kl10 (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tplnr TEXT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
            database.execSQL("""
                    CREATE TABLE IF NOT EXISTS abon_kl04 (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tplnr TEXT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
            database.execSQL("""
                    CREATE TABLE IF NOT EXISTS abon_kl10 (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tplnr TEXT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())

            database.execSQL("""
                ALTER TABLE line_pillar ADD COLUMN recloser TEXT
            """.trimIndent())
        }
    }
}