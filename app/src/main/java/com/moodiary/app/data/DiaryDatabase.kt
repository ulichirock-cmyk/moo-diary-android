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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One row per entry. [blocks] is the body as a JSON array of `{"text": …}` /
 * `{"photo": …, "caption"?: …}` objects in reading order; [text] and [photos] are kept alongside as
 * the derived prose and photo list so the columns stay truthful and readable in a
 * SQLite browser. [tags] is a JSON array too: all three are only ever read back whole,
 * so a join table would be machinery for nothing. [createdAt] is epoch millis so
 * newest-first is a plain `ORDER BY`.
 *
 * [blocks] is null on rows written before version 2; those read back as text-then-photos.
 */
@Entity(tableName = "entries")
data class DiaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val text: String,
    val photos: String,
    val tags: String,
    val place: String?,
    val blocks: String? = null,
) {
    fun toEntry() = DiaryEntry(
        id = id,
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZONE),
        blocks = blocks?.toBlocks() ?: blocksOf(text, photos.toStringList()),
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
            blocks = entry.blocks.toBlocksJson(),
        )

        private fun List<Block>.toBlocksJson(): String = JSONArray().also { array ->
            forEach { block ->
                array.put(
                    when (block) {
                        is Block.Text -> JSONObject().put("text", block.text)
                        is Block.Photo -> JSONObject().put("photo", block.uri)
                            .also { o -> block.caption?.let { o.put("caption", it) } }
                    },
                )
            }
        }.toString()

        private fun String.toBlocks(): List<Block> {
            val array = JSONArray(this)
            return List(array.length()) { i ->
                val o = array.getJSONObject(i)
                if (o.has("photo")) {
                    Block.Photo(o.getString("photo"), o.optString("caption").takeIf { it.isNotBlank() })
                } else {
                    Block.Text(o.optString("text"))
                }
            }
        }

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

@Database(entities = [DiaryEntity::class], version = 2, exportSchema = false)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile private var instance: DiaryDatabase? = null

        /** Version 2 adds the nullable `blocks` column; existing rows keep reading as text-then-photos. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN blocks TEXT")
            }
        }

        fun get(context: Context): DiaryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DiaryDatabase::class.java,
                "moodiary.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
