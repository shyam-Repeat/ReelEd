package com.reeled.quizoverlay.data.local.dao

import androidx.room.*
import com.reeled.quizoverlay.data.local.entity.OverlaySessionEntity

@Dao
interface OverlaySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: OverlaySessionEntity)

    @Update
    suspend fun update(session: OverlaySessionEntity)

    @Query("SELECT * FROM overlay_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: String): OverlaySessionEntity?

    @Query("SELECT * FROM overlay_sessions WHERE synced = 0")
    suspend fun getUnsynced(): List<OverlaySessionEntity>

    @Query("UPDATE overlay_sessions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}
