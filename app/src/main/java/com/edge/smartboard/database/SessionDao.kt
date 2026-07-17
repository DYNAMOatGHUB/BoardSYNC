package com.edge.smartboard.database

import androidx.room.*
import com.edge.smartboard.models.Session
import com.edge.smartboard.models.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE sessionId = :id")
    suspend fun getSession(id: String): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM sessions WHERE status = :status")
    fun getSessionsByStatus(status: SessionStatus): Flow<List<Session>>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getTotalCount(): Int

    @Query("SELECT SUM(storageBytes) FROM sessions")
    suspend fun getTotalStorageBytes(): Long?
}
