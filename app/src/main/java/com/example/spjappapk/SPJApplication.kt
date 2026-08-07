package com.example.spjappapk

import android.app.Application
import com.example.spjappapk.data.local.AppDatabase
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.InventoryRepository
import com.example.spjappapk.data.repository.SalesRepository
import com.example.spjappapk.data.repository.VisitRepository
import com.example.spjappapk.data.session.SessionManager
import com.example.spjappapk.data.sync.SyncManager

class SPJApplication : Application() {
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    val sessionManager: SessionManager by lazy { SessionManager(this) }

    override fun onCreate() {
        super.onCreate()
        
        // Setup global error handling for easier debugging on physical devices
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SPJ_FATAL", "Uncaught exception on thread ${thread.name}", throwable)
            // Default behavior will still show the "App kept stopping" dialog
        }

        // Memulai sinkronisasi berkala setiap jam
        SyncManager.schedulePeriodicSync(this)
    }
    
    val inventoryRepository: InventoryRepository by lazy { 
        InventoryRepository(database.inventoryDao(), SupabaseClient.client) 
    }

    val customerRepository: CustomerRepository by lazy {
        CustomerRepository(database.customerDao(), SupabaseClient.client)
    }
    
    val salesRepository: SalesRepository by lazy { 
        SalesRepository(database.salesOrderDao(), this) 
    }
    
    val visitRepository: VisitRepository by lazy { 
        VisitRepository(database.visitDao(), this) 
    }
}
