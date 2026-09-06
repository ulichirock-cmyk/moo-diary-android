package com.moodiary.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [DiaryRepository] backed by Room. [entries] mirrors the table as a hot StateFlow so
 * the UI keeps reading a plain list; writes go to the database on IO and the flow
 * pushes the new list back.
 *
 * The write API stays synchronous to match the interface: the caller fires and moves
 * on, and the timeline updates when the row lands. The scope outlives any screen or
 * view model — a write started from the editor must finish even if the editor closes.
 *
 * The first run seeds the table from [seedEntries] so the app opens with the same
 * sample diary it always did; after that the sample is just data the user can delete.
 * "First run" is a flag, not an empty table — 恢复出厂设置 leaves the table empty on
 * purpose, and the samples must not walk back in on the next launch.
 */
class RoomDiaryRepository private constructor(context: Context) : DiaryRepository {

    private val dao = DiaryDatabase.get(context).diaryDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("diary", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val entries: StateFlow<List<DiaryEntry>> = dao.observeAll()
        .map { rows -> rows.map(DiaryEntity::toEntry) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            if (!prefs.getBoolean(KEY_SEEDED, false)) {
                if (dao.count() == 0) dao.upsertAll(seedEntries().map(DiaryEntity::from))
                prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            }
        }
    }

    override fun upsert(entry: DiaryEntry) {
        scope.launch { dao.upsert(DiaryEntity.from(entry)) }
    }

    override fun delete(id: String) {
        scope.launch { dao.delete(id) }
    }

    override fun clear() {
        scope.launch {
            dao.deleteAll()
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
    }

    companion object {
        private const val KEY_SEEDED = "seeded"

        @Volatile private var instance: RoomDiaryRepository? = null

        fun get(context: Context): RoomDiaryRepository = instance ?: synchronized(this) {
            instance ?: RoomDiaryRepository(context.applicationContext).also { instance = it }
        }
    }
}
