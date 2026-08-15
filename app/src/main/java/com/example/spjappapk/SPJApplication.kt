package com.example.spjappapk

import android.app.Application
import com.example.spjappapk.data.local.AppDatabase
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.InventoryRepository
import com.example.spjappapk.data.repository.NotificationRepository
import com.example.spjappapk.data.repository.SalesRepository
import com.example.spjappapk.data.repository.VisitRepository
import com.example.spjappapk.data.session.SessionManager
import com.example.spjappapk.data.sync.SyncManager

class SPJApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    val sessionManager: SessionManager by lazy { SessionManager(this) }

    override fun onCreate() {
        super.onCreate()
        
        // Setup global error handling for easier debugging on physical devices
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTraceString = sw.toString()
                
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val report = "Crash Timestamp: $timestamp\n\n$stackTraceString"
                
                val file = java.io.File(filesDir, "latest_crash.txt")
                file.writeText(report)
            } catch (e: Exception) {
                // Fail silently if we can't write the crash log
                android.util.Log.e("SPJ_ERROR", "Failed to save crash log", e)
            }
            
            android.util.Log.e("SPJ_FATAL", "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Memulai sinkronisasi berkala setiap jam
        SyncManager.schedulePeriodicSync(this)
    }
    
    val inventoryRepository: InventoryRepository by lazy { 
        InventoryRepository(database.inventoryDao(), database.syncLogDao(), SupabaseClient.client) 
    }

    val customerRepository: CustomerRepository by lazy {
        CustomerRepository(database.customerDao(), database.syncLogDao(), SupabaseClient.client)
    }
    
    val salesRepository: SalesRepository by lazy { 
        SalesRepository(database.salesOrderDao(), database.salesMasterDao(), database.salesDetailDao(), database.salesPersonDao(), this) 
    }
    
    val visitRepository: VisitRepository by lazy { 
        VisitRepository(database.visitDao(), this) 
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(database.notificationDao())
    }
}
