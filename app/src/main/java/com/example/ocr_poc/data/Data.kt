package com.example.ocr_poc.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ocr_poc.models.EditableTextEntity
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

@Dao
interface TextEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EditableTextEntity>)

    @Query("SELECT * FROM text_entities")
    suspend fun getAllEntities(): List<EditableTextEntity> // Ensure this is a List, not Object

    @Update
    suspend fun updateEntity(entity: EditableTextEntity)
}

@Database(entities = [EditableTextEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun textEntityDao(): TextEntityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "text_entities_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}