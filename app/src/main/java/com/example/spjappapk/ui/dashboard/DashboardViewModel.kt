package com.example.spjappapk.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.SalesOrderItem
import com.example.spjappapk.data.local.entities.deduplicateVisits
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.InventoryRepository
import com.example.spjappapk.data.repository.SalesRepository
import com.example.spjappapk.data.repository.VisitRepository
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import com.example.spjappapk.data.remote.models.AppUpdate
import com.example.spjappapk.util.DateTimeUtil
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.*

data class DashboardUiState(
    val totalOrdersToday: Int = 0,
    val estimatedOmzetToday: Double = 0.0,
    val monthlyOmzet: Double = 0.0,
    val totalVisitsToday: Int = 0,
    val monthlyMetrics: com.example.spjappapk.util.DashboardMetrics = com.example.spjappapk.util.DashboardMetrics(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncStatus: String = "Idle",
    val userName: String = "User",
    val userRole: String = "",
    val appUpdate: AppUpdate? = null,
    val teamBreakdown: List<com.example.spjappapk.data.local.dao.SalesPersonOmzet> = emptyList()
)

class DashboardViewModel(
    val salesRepository: SalesRepository,
    val visitRepository: VisitRepository,
    val inventoryRepository: InventoryRepository,
    val customerRepository: CustomerRepository,
    val currentUser: SalesPerson?,
    val app: com.example.spjappapk.SPJApplication
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val _isSyncing = MutableStateFlow(false)
    private val _syncStatus = MutableStateFlow("Idle")
    private val _appUpdate = MutableStateFlow<AppUpdate?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        salesRepository.allSales,
        salesRepository.allSalesMaster, // Pantau perubahan data master (omset)
        visitRepository.allVisits,
        app.database.salesPersonDao().getAllSalesPersons(), // Untuk resolusi nama tim
        _isSyncing,
        _syncStatus,
        _appUpdate
    ) { flows ->
        val salesQueue = flows[0] as List<com.example.spjappapk.data.local.entities.SalesOrder>
        val salesMaster = flows[1] as List<com.example.spjappapk.data.local.entities.SalesMaster>
        val visits = flows[2] as List<com.example.spjappapk.data.local.entities.Visit>
        val allStaff = flows[3] as List<com.example.spjappapk.data.local.entities.SalesPersonEntity>
        val isSyncing = flows[4] as Boolean
        val syncStatus = flows[5] as String
        val appUpdate = flows[6] as AppUpdate?

        try {
            if (currentUser == null) return@combine DashboardUiState(isLoading = false, isSyncing = isSyncing, syncStatus = syncStatus)

            android.util.Log.d("DashboardVM", "Current Default Timezone: ${java.util.TimeZone.getDefault().id}")
            
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"))
            val todayStr = com.example.spjappapk.util.DateTimeUtil.getFormat("yyyy-MM-dd").format(calendar.time)
            
            // Start of Month
            val startOfMonthCal = calendar.clone() as Calendar
            startOfMonthCal.set(Calendar.DAY_OF_MONTH, 1)
            startOfMonthCal.set(Calendar.HOUR_OF_DAY, 0)
            startOfMonthCal.set(Calendar.MINUTE, 0)
            startOfMonthCal.set(Calendar.SECOND, 0)
            startOfMonthCal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = startOfMonthCal.timeInMillis

            // End of Month
            val endOfMonthCal = calendar.clone() as Calendar
            endOfMonthCal.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            endOfMonthCal.set(Calendar.HOUR_OF_DAY, 23)
            endOfMonthCal.set(Calendar.MINUTE, 59)
            endOfMonthCal.set(Calendar.SECOND, 59)
            endOfMonthCal.set(Calendar.MILLISECOND, 999)
            val endOfMonth = endOfMonthCal.timeInMillis

            val roleClean = currentUser.role?.trim()?.lowercase() ?: ""
            val isManagerOrOwner = roleClean in listOf("manager", "owner", "admin")

            // 1. Hitung Pesanan Hari Ini (Queue/Antrean)
            val todayOrders = if (isManagerOrOwner) salesQueue else salesQueue.filter { 
                it.salesPersonId.equals(currentUser.id, ignoreCase = true) || 
                it.salesPersonName.equals(currentUser.name, ignoreCase = true)
            }
            val omzetToday = todayOrders.filter { 
                try { com.example.spjappapk.util.DateTimeUtil.getFormat("yyyy-MM-dd").format(Date(it.timestamp)) == todayStr } catch(e:Exception){false}
            }.sumOf { order ->
                try {
                    val items = json.decodeFromString<List<SalesOrderItem>>(order.details)
                    items.sumOf { it.displayPrice * it.quantity }
                } catch (e: Exception) { 0.0 }
            }

            // 2. Hitung Metrik Dashboard (Sesuai SOP SPJ menggunakan Helper)
            val metrics = com.example.spjappapk.util.BusinessRevenueHelper.calculateDashboardSummary(
                salesList = salesMaster,
                salesPersonList = allStaff,
                targetMonth = calendar.get(Calendar.MONTH) + 1,
                targetYear = calendar.get(Calendar.YEAR),
                currentUser = currentUser
            )

            // 3. Breakdown Tim (Manager Only)
            val breakdown = if (isManagerOrOwner) {
                metrics.salesBreakdown.map { (name, amount) ->
                    com.example.spjappapk.data.local.dao.SalesPersonOmzet(name, amount)
                }.sortedByDescending { it.totalAmount }
            } else emptyList()

            // 4. Kunjungan Hari Ini
            val deduplicatedVisits = visits.deduplicateVisits()
            val startOfDay = DateTimeUtil.getStartOfDay()
            val endOfDay = DateTimeUtil.getEndOfDay()

            val vToday = (if (isManagerOrOwner) deduplicatedVisits else deduplicatedVisits.filter { 
                it.userId.equals(currentUser.id, ignoreCase = true) || 
                it.salesPersonName.equals(currentUser.name, ignoreCase = true)
            }).filter { it.timestamp in startOfDay..endOfDay }

            DashboardUiState(
                totalOrdersToday = todayOrders.size,
                estimatedOmzetToday = omzetToday,
                monthlyOmzet = metrics.totalOmset,
                totalVisitsToday = vToday.size,
                monthlyMetrics = metrics,
                isLoading = false,
                isSyncing = isSyncing,
                syncStatus = syncStatus,
                userName = currentUser.name,
                userRole = currentUser.role ?: "",
                appUpdate = appUpdate,
                teamBreakdown = breakdown
            )
        } catch (e: Exception) {
            android.util.Log.e("DashboardVM", "UI Combine Error", e)
            DashboardUiState(isLoading = false, syncStatus = "Error: ${e.localizedMessage}")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(isLoading = true))

    init {
        manualSync()
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
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
                }
            } catch (e: Exception) { }
        }
    }

    fun sync() {
        salesRepository.scheduleSync()
        visitRepository.scheduleSync()
    }

    fun manualSync() {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            _syncStatus.value = "Menghubungkan ke server..."
            try {
                inventoryRepository.refreshInventory()
                customerRepository.refreshCustomers()
                // Refresh master sales explicitly (Gunakan Full Sync untuk memastikan data tidak terpotong)
                if (currentUser != null) {
                    val role = currentUser.role?.trim()?.lowercase() ?: ""
                    val isManagerOrOwner = role in listOf("manager", "owner", "admin")
                    salesRepository.refreshSalesMaster(currentUser.id, isManagerOrOwner, isFullSync = true)
                }
                sync()
                checkForUpdates()
                _syncStatus.value = "Sinkronisasi selesai"
            } catch (e: Exception) {
                _syncStatus.value = "Gagal Sinkron: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
