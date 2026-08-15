package com.example.spjappapk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.spjappapk.data.local.entities.SyncLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insert(log: SyncLog)

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<SyncLog>>

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}
