package com.example.spjappapk.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spjappapk.data.local.dao.VisitDao
import com.example.spjappapk.data.local.entities.Visit
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.sync.SyncWorker
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class VisitRepository(
    private val visitDao: VisitDao,
    private val context: Context
) {
    val allVisits: Flow<List<Visit>> = visitDao.getAllVisits()

    suspend fun refreshVisits(salesPersonId: String, isFullSync: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val lastUpdate = if (isFullSync) null else visitDao.getMaxCheckInDate()
            val response = SupabaseClient.client.postgrest["check_in_logs"]
                .select {
                    filter { eq("sales_person_id", salesPersonId) }
                    if (isFullSync) {
                        limit(5000)
                    } else if (lastUpdate != null) {
                        filter { gt("check_in_date", lastUpdate) }
                    }
                }
            
            val data = Json { ignoreUnknownKeys = true }.decodeFromString<List<Visit>>(response.data)
            
            // Mark as SUCCESS and convert check_in_date to timestamp for display
            val markedData = data.map { visit ->
                val timestampFromDate = com.example.spjappapk.util.DateTimeUtil.parseIso(visit.checkInDate ?: "")?.time 
                    ?: visit.timestamp
                visit.copy(syncStatus = "SUCCESS", timestamp = timestampFromDate)
            }
            
            if (isFullSync) {
                visitDao.clearSyncedVisits()
            }
            visitDao.insertAll(markedData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertVisit(visit: Visit) {
        visitDao.insertVisit(visit)
        scheduleSync()
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    suspend fun deleteVisit(visitId: Long) {
        visitDao.deleteVisit(visitId)
    }

    suspend fun updateVisitNotes(visitId: Long, notes: String) {
        visitDao.updateNotes(visitId, notes)
    }
}
