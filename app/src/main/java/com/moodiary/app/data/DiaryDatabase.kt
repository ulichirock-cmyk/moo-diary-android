package com.moodiary.app.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One row per entry. [photos] and [tags] are JSON arrays in a text column: they are
 * only ever read back as a whole list, so a join table would be machinery for nothing.
 * [createdAt] is epoch millis so newest-first is a plain `ORDER BY`.
 */
@Entity(tableName = "entries")
data class DiaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val text: String,
    val photos: String,
    val tags: String,
    val place: String?,
) {
    fun toEntry() = DiaryEntry(
        id = id,
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZONE),
        text = text,
        photos = photos.toStringList(),
        tags = tags.toStringList(),
        place = place,
    )

    companion object {
        private val ZONE: ZoneId = ZoneId.systemDefault()

        fun from(entry: DiaryEntry) = DiaryEntity(
            id = entry.id,
            createdAt = entry.createdAt.atZone(ZONE).toInstant().toEpochMilli(),
            text = entry.text,
            photos = entry.photos.toJson(),
            tags = entry.tags.toJson(),
            place = entry.place,
        )

        private fun List<String>.toJson() = JSONArray(this).toString()
        private fun String.toStringList(): List<String> {
            val array = JSONArray(this)
            return List(array.length()) { array.getString(it) }
        }
    }
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM entries ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DiaryEntity>>

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DiaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DiaryEntity>)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [DiaryEntity::class], version = 1, exportSchema = false)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile private var instance: DiaryDatabase? = null

        fun get(context: Context): DiaryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DiaryDatabase::class.java,
                "moodiary.db",
            ).build().also { instance = it }
        }
    }
}
