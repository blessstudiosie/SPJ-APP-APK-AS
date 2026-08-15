package com.example.spjappapk.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spjappapk.data.local.AppDatabase
import com.example.spjappapk.data.local.entities.SyncLog
import com.example.spjappapk.data.sync.SyncWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SyncLogViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val syncLogDao = database.syncLogDao()

    val syncLogs: StateFlow<List<SyncLog>> = syncLogDao.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun triggerSync() {
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(getApplication()).enqueue(workRequest)
    }

    fun clearLogs() {
        viewModelScope.launch {
            syncLogDao.clearLogs()
        }
    }
}
