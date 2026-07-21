package com.edge.smartboard.database

import androidx.room.*
import com.edge.smartboard.models.LocalRecording
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalRecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: LocalRecording)

    @Query("SELECT * FROM local_recordings ORDER BY recordedAt DESC")
    fun getAllFlow(): Flow<List<LocalRecording>>

    @Query("SELECT * FROM local_recordings ORDER BY recordedAt DESC")
    suspend fun getAll(): List<LocalRecording>

    @Query("DELETE FROM local_recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM local_recordings")
    suspend fun count(): Int

    @Query("SELECT SUM(fileSizeBytes) FROM local_recordings")
    suspend fun totalBytes(): Long?
}
