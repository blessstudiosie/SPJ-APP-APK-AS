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
import com.example.spjappapk.util.DateTimeUtil
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
            val allData = mutableListOf<Visit>()
            var offset = 0
            val pageSize = 1000

            while (true) {
                val response = SupabaseClient.client.postgrest["check_in_logs"]
                    .select {
                        filter { eq("sales_person_id", salesPersonId) }
                        if (lastUpdate != null) {
                            filter { gt("check_in_date", lastUpdate) }
                        }
                        range(offset.toLong(), (offset + pageSize - 1).toLong())
                    }
                val batch = Json { ignoreUnknownKeys = true }.decodeFromString<List<Visit>>(response.data)
                allData.addAll(batch)
                
                if (batch.size < pageSize) break
                offset += pageSize
            }
            
            val existingVisits = visitDao.getAllVisitsList()
            val existingByRemoteId = existingVisits.filter { !it.remoteId.isNullOrBlank() }.associateBy { it.remoteId }

            // Mark as SUCCESS, reuse local ID if matched to avoid duplicate PKs, and convert check_in_date to timestamp for display
            val markedData = allData.map { remoteVisit ->
                val localMatch = if (!remoteVisit.remoteId.isNullOrBlank()) existingByRemoteId[remoteVisit.remoteId] else null
                val timestampFromDate = DateTimeUtil.parseIso(remoteVisit.checkInDate ?: "")?.time
                    ?: remoteVisit.timestamp

                remoteVisit.copy(
                    id = localMatch?.id ?: 0L,
                    photoPath = localMatch?.photoPath ?: remoteVisit.photoPath,
                    syncStatus = "SUCCESS",
                    timestamp = timestampFromDate
                )
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

    suspend fun getTodayVisitForCustomer(customerId: String): Visit? = withContext(Dispatchers.IO) {
        val startOfDay = com.example.spjappapk.util.DateTimeUtil.getStartOfDay()
        val endOfDay = com.example.spjappapk.util.DateTimeUtil.getEndOfDay()
        visitDao.getTodayVisitForCustomer(customerId, startOfDay, endOfDay)
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
