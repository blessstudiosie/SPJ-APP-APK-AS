package com.example.spjappapk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.InventoryRepository
import com.example.spjappapk.data.repository.SalesRepository
import com.example.spjappapk.data.repository.VisitRepository
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.remote.models.AppUpdate
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
    private val salesRepository: SalesRepository,
    private val visitRepository: VisitRepository,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("")
    val syncStatus = _syncStatus.asStateFlow()

    private val _appUpdate = MutableStateFlow<AppUpdate?>(null)
    val appUpdate = _appUpdate.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun checkForUpdate() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Mengecek versi aplikasi..."
            try {
                val response = SupabaseClient.client.postgrest["app_versions"]
                    .select {
                        filter { eq("platform", "android") }
                        order("version_code", Order.DESCENDING)
                        limit(1)
                    }
                val updates = json.decodeFromString<List<AppUpdate>>(response.data)
                if (updates.isNotEmpty() && updates[0].versionCode > com.example.spjappapk.BuildConfig.VERSION_CODE) {
                    _appUpdate.value = updates[0]
                    _syncStatus.value = "Versi baru tersedia: ${updates[0].versionName}"
                } else {
                    _syncStatus.value = "Aplikasi sudah versi terbaru."
                }
            } catch (e: Exception) {
                _syncStatus.value = "Gagal cek update: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearUpdateState() {
        _appUpdate.value = null
    }

    fun fullResetAndSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                _syncStatus.value = "Membersihkan data lokal..."
                inventoryRepository.clearAndRefresh()
                
                _syncStatus.value = "Menarik data pelanggan..."
                customerRepository.clearAndRefresh()
                
                _syncStatus.value = "Menarik data tim (sales)..."
                salesRepository.refreshSalesPersons(isFullSync = true)

                _syncStatus.value = "Menarik data nota final..."
                if (currentUser != null) {
                    val isManagerOrOwner = currentUser.role?.lowercase() == "manager" || currentUser.role?.lowercase() == "owner"
                    salesRepository.clearAndRefreshSalesMaster(
                        currentUser.id,
                        isManagerOrOwner
                    )
                }

                _syncStatus.value = "Menarik data kunjungan..."
                if (currentUser != null) {
                    visitRepository.refreshVisits(currentUser.id, isFullSync = true)
                }
                
                _syncStatus.value = "Sinkronisasi selesai!"
            } catch (e: Exception) {
                _syncStatus.value = "Gagal: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun fixOrphanVisits() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Memindai antrean kunjungan..."
            try {
                val unsynced = visitRepository.allVisits.first().filter { 
                    it.syncStatus == "PENDING" || it.syncStatus == "ERROR" 
                }
                
                if (unsynced.isEmpty()) {
                    _syncStatus.value = "Tidak ada antrean bermasalah."
                } else {
                    _syncStatus.value = "Memperbaiki ${unsynced.size} data..."
                    unsynced.forEach { visit ->
                        customerRepository.checkAndFixOrphanCustomer(
                            visit.customerId,
                            visit.customerName,
                            visit.userId,
                            visit.salesPersonName
                        )
                    }
                    _syncStatus.value = "Perbaikan selesai. Menjalankan sinkronisasi..."
                    visitRepository.scheduleSync()
                }
            } catch (e: Exception) {
                _syncStatus.value = "Gagal perbaikan: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
