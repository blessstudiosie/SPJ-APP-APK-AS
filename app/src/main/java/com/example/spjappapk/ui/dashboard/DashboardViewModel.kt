package com.example.spjappapk.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.SalesOrderItem
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.InventoryRepository
import com.example.spjappapk.data.repository.SalesRepository
import com.example.spjappapk.data.repository.VisitRepository
import com.example.spjappapk.ui.auth.SalesPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

data class DashboardUiState(
    val totalOrdersToday: Int = 0,
    val estimatedOmzetToday: Double = 0.0,
    val totalVisitsToday: Int = 0,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncStatus: String = "Idle",
    val userName: String = ""
)

class DashboardViewModel(
    private val salesRepository: SalesRepository,
    private val visitRepository: VisitRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    private val _isSyncing = MutableStateFlow(false)
    private val _syncStatus = MutableStateFlow("Idle")

    val uiState: StateFlow<DashboardUiState> = combine(
        salesRepository.allSales,
        visitRepository.allVisits,
        _isSyncing,
        _syncStatus
    ) { sales, visits, isSyncing, syncStatus ->
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormatter.format(Date())
        
        // Filter based on role
        val filteredSales = if (currentUser?.role?.lowercase() == "manager") {
            sales
        } else {
            sales.filter { it.salesPersonId == currentUser?.id }
        }

        val filteredVisits = if (currentUser?.role?.lowercase() == "manager") {
            visits
        } else {
            visits.filter { it.userId == currentUser?.id }
        }

        // Calculate for today
        val salesToday = filteredSales.filter { 
            dateFormatter.format(Date(it.timestamp)) == today 
        }
        
        var omzet = 0.0
        salesToday.forEach { order ->
            try {
                val items = json.decodeFromString<List<SalesOrderItem>>(order.details)
                omzet += items.sumOf { it.price * it.quantity }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val visitsToday = filteredVisits.filter { 
            dateFormatter.format(Date(it.timestamp)) == today 
        }.size

        DashboardUiState(
            totalOrdersToday = salesToday.size,
            estimatedOmzetToday = omzet,
            totalVisitsToday = visitsToday,
            isSyncing = isSyncing,
            syncStatus = syncStatus,
            userName = currentUser?.name ?: "User"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(isLoading = true))

    init {
        manualSync() // Start sync immediately
    }

    fun sync() {
        salesRepository.scheduleSync()
        visitRepository.scheduleSync()
    }

    fun manualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Menghubungkan ke server..."
            
            try {
                _syncStatus.value = "Mengunduh data stok..."
                inventoryRepository.refreshInventory()
                
                _syncStatus.value = "Mengunduh data pelanggan..."
                customerRepository.refreshCustomers()
                
                _syncStatus.value = "Sinkronisasi antrean..."
                sync()
                
                _syncStatus.value = "Sinkronisasi selesai"
            } catch (e: Exception) {
                _syncStatus.value = "Gagal Sinkron: ${e.localizedMessage ?: "Cek koneksi internet"}"
                android.util.Log.e("DashboardVM", "Sync error", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun refreshMasterData() {
        // Obsolete, use manualSync
    }
}
