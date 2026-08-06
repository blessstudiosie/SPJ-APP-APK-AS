package com.example.spjappapk.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spjappapk.data.local.dao.VisitDao
import com.example.spjappapk.data.local.entities.Visit
import com.example.spjappapk.data.sync.SyncWorker
import kotlinx.coroutines.flow.Flow

class VisitRepository(
    private val visitDao: VisitDao,
    private val context: Context
) {
    val allVisits: Flow<List<Visit>> = visitDao.getAllVisits()

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
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }
}
