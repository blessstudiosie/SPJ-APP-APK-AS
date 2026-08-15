package com.example.spjappapk.ui.error

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.SPJApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class ErrorReportViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SPJApplication
    
    var errorText by mutableStateOf<String?>(null)
        private set

    var syncLogsText by mutableStateOf<String?>(null)
        private set

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus = _uploadStatus.asStateFlow()

    private val client = OkHttpClient()
    // URL Web App Google Apps Script
    private val APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzf_N0YQxXv7_P-U4H_7_Z_REPLACE_ME/exec"

    init {
        loadErrorReport()
    }

    fun loadErrorReport() {
        val crashFile = File(getApplication<Application>().filesDir, "latest_crash.txt")
        errorText = if (crashFile.exists()) crashFile.readText() else null

        viewModelScope.launch {
            val logs = app.database.syncLogDao().getRecentLogs().firstOrNull()?.take(20)
            syncLogsText = logs?.joinToString("\n") { 
                "[${it.status}] ${it.event}: ${it.message}" 
            }
        }
    }

    fun sendReport(userFeedback: String? = null) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadStatus.value = "Mengunggah..."
            
            val content = StringBuilder()
            content.append("--- LAPORAN DARI APLIKASI SPJ ---\n")
            content.append("Waktu: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
            
            val user = app.sessionManager.userFlow.firstOrNull()
            content.append("User: ${user?.name ?: "Unknown"}\n")
            content.append("Role: ${user?.role ?: "Unknown"}\n\n")
            
            if (!userFeedback.isNullOrBlank()) {
                content.append("FEEDBACK USER:\n$userFeedback\n\n")
            }
            
            if (!errorText.isNullOrBlank()) {
                content.append("--- CRASH LOG ---\n$errorText\n\n")
            }
            
            if (!syncLogsText.isNullOrBlank()) {
                content.append("--- RECENT SYNC LOGS ---\n$syncLogsText\n\n")
            }

            val fileName = if (userFeedback != null) "Feedback_${user?.name}_${System.currentTimeMillis()}.txt" else "Report_${user?.name}_${System.currentTimeMillis()}.txt"
            
            val result = uploadToGoogleDrive(fileName, content.toString())
            
            if (result == "Success") {
                _uploadStatus.value = "Berhasil Terkirim ke Google Drive!"
                if (userFeedback == null) clearReport()
            } else {
                _uploadStatus.value = "Gagal: $result"
            }
            _isUploading.value = false
        }
    }

    private suspend fun uploadToGoogleDrive(fileName: String, content: String): String = withContext(Dispatchers.IO) {
        try {
            if (APPS_SCRIPT_URL.contains("REPLACE_ME")) {
                return@withContext "URL Apps Script belum dikonfigurasi."
            }

            val formBody = FormBody.Builder()
                .add("filename", fileName)
                .add("content", content)
                .add("folderId", "1vHCkdzi2QEReJ3hoq7pAd7xg49fytZ4z")
                .build()

            val request = Request.Builder()
                .url(APPS_SCRIPT_URL)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    "Success"
                } else {
                    "Server Error: ${response.code} ${response.message}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Network Error: ${e.localizedMessage}"
        }
    }

    fun clearReport() {
        val crashFile = File(getApplication<Application>().filesDir, "latest_crash.txt")
        if (crashFile.exists()) crashFile.delete()
        
        errorText = null
        syncLogsText = null
        _uploadStatus.value = null
    }
}
