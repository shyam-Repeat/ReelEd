package com.reeled.quizoverlay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reeled.quizoverlay.data.local.entity.EventLogEntity

@Dao
interface EventLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventLogEntity)

    @Query("SELECT * FROM event_logs WHERE synced = 0")
    suspend fun getUnsynced(): List<EventLogEntity>

    @Query("UPDATE event_logs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT * FROM event_logs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<EventLogEntity>
}
