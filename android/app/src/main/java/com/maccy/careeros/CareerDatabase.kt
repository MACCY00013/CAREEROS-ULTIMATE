package com.maccy.careeros

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "skills")
data class SkillEntity(
    @androidx.room.PrimaryKey val name: String,
    val level: String = "learning",
    val syncStatus: String = "PENDING",
)

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY name")
    suspend fun all(): List<SkillEntity>

    @Query("SELECT * FROM skills WHERE syncStatus = 'PENDING'")
    suspend fun pending(): List<SkillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(skill: SkillEntity)

    @Query("UPDATE skills SET syncStatus = 'SYNCED' WHERE name = :name")
    suspend fun markSynced(name: String)
}

@Database(entities = [SkillEntity::class], version = 1, exportSchema = false)
abstract class CareerDatabase : RoomDatabase() {
    abstract fun skillDao(): SkillDao

    companion object {
        @Volatile private var instance: CareerDatabase? = null

        fun get(context: Context): CareerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, CareerDatabase::class.java, "careeros.db").build().also { instance = it }
        }
    }
}