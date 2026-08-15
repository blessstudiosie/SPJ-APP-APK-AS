package com.example.spjappapk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spjappapk.data.local.entities.Visit
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY timestamp DESC")
    fun getAllVisits(): Flow<List<Visit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: Visit): Long

    @Query("SELECT * FROM visits WHERE sync_status = 'PENDING'")
    suspend fun getUnsyncedVisits(): List<Visit>

    @Query("SELECT * FROM visits WHERE remote_id IS NOT NULL AND sync_status = 'PROCESSED'")
    suspend fun getVisitsAwaitingFeedback(): List<Visit>

    @Query("UPDATE visits SET sync_status = :status, remote_id = :remoteId WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, remoteId: String?)

    @Query("UPDATE visits SET sync_status = :status WHERE remote_id = :remoteId")
    suspend fun updateSyncStatusByRemoteId(remoteId: String, status: String)

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteVisit(id: Long)

    @Query("UPDATE visits SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(visits: List<Visit>)

    @Query("DELETE FROM visits WHERE sync_status = 'SUCCESS' OR sync_status = 'SYNCED'")
    suspend fun clearSyncedVisits()

    @Query("SELECT MAX(check_in_date) FROM visits")
    suspend fun getMaxCheckInDate(): String?
}
