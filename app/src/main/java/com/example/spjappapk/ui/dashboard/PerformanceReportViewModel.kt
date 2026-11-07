package com.example.spjappapk.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.data.local.models.CompanyPerformanceSummary
import com.example.spjappapk.data.local.models.SalesPersonPerformance
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.util.BusinessRevenueHelper
import com.example.spjappapk.util.PerformanceCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class PerformanceReportUiState(
    val selectedTab: Int = 0, // 0: Keseluruhan, 1: Per Sales, 2: Pribadi
    val isManagerOrOwner: Boolean = false,
    val summary: CompanyPerformanceSummary = CompanyPerformanceSummary(),
    val myPerformance: SalesPersonPerformance = SalesPersonPerformance("", ""),
    val isLoading: Boolean = false,
    val isLoadingDetails: Boolean = false
)

class PerformanceReportViewModel(
    private val app: SPJApplication,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow(false)

    private val isManagerOrOwner = currentUser?.role?.trim()?.lowercase() in listOf("manager", "owner", "admin")

    init {
        // Sales role gets locked to Tab 2 ("Pribadi")
        if (!isManagerOrOwner) {
            _selectedTab.value = 2
        }

        viewModelScope.launch {
            app.salesRepository.allSalesMaster.collectLatest { salesMaster ->
                val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+08:00"))
                val targetMonth = calendar.get(Calendar.MONTH) + 1
                val targetYear = calendar.get(Calendar.YEAR)

                val monthlySaleIds = salesMaster.filter { sale ->
                    if (sale.isDeleted == true || !BusinessRevenueHelper.isValidOmsetStatus(sale.status)) {
                        return@filter false
                    }
                    val effTimestamp = sale.deliveryDate ?: sale.orderDate
                    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+08:00")).apply {
                        timeInMillis = effTimestamp
                    }
                    val m = cal.get(Calendar.MONTH) + 1
                    val y = cal.get(Calendar.YEAR)
                    m == targetMonth && y == targetYear
                }.map { it.id }

                if (monthlySaleIds.isNotEmpty()) {
                    _isLoadingDetails.value = true
                    app.salesRepository.prefetchSalesDetails(monthlySaleIds)
                    _isLoadingDetails.value = false
                }
            }
        }
    }

    val uiState: StateFlow<PerformanceReportUiState> = combine(
        app.salesRepository.allSalesMaster,
        app.salesRepository.allSales,
        app.visitRepository.allVisits,
        app.customerRepository.allCustomers,
        app.database.salesPersonDao().getAllSalesPersons(),
        app.salesRepository.allSalesDetails,
        _selectedTab,
        _isLoadingDetails
    ) { flows ->
        val salesMaster = flows[0] as List<com.example.spjappapk.data.local.entities.SalesMaster>
        val localQueueOrders = flows[1] as List<com.example.spjappapk.data.local.entities.SalesOrder>
        val visits = flows[2] as List<com.example.spjappapk.data.local.entities.Visit>
        val customers = flows[3] as List<com.example.spjappapk.data.local.entities.Customer>
        val staffList = flows[4] as List<com.example.spjappapk.data.local.entities.SalesPersonEntity>
        val salesDetails = flows[5] as List<com.example.spjappapk.data.local.entities.SalesDetailEntity>
        val currentTab = flows[6] as Int
        val loadingDetails = flows[7] as Boolean

        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+08:00"))
        val targetMonth = calendar.get(Calendar.MONTH) + 1
        val targetYear = calendar.get(Calendar.YEAR)

        val summary = PerformanceCalculator.calculatePerformance(
            salesMasterList = salesMaster,
            visitsList = visits,
            customersList = customers,
            salesPersonEntities = staffList,
            currentUser = currentUser,
            targetMonth = targetMonth,
            targetYear = targetYear,
            salesDetailsList = salesDetails,
            localQueueOrders = localQueueOrders
        )

        val myPerf = summary.salesBreakdown.find { 
            it.salesPersonId.equals(currentUser?.id, ignoreCase = true) ||
            it.salesPersonName.equals(currentUser?.name, ignoreCase = true)
        } ?: SalesPersonPerformance(
            salesPersonId = currentUser?.id ?: "",
            salesPersonName = currentUser?.name ?: "Saya",
            targetOmset = currentUser?.targetOmset ?: 0.0
        )

        PerformanceReportUiState(
            selectedTab = if (isManagerOrOwner) currentTab else 2,
            isManagerOrOwner = isManagerOrOwner,
            summary = summary,
            myPerformance = myPerf,
            isLoading = false,
            isLoadingDetails = loadingDetails
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerformanceReportUiState(isLoading = true))

    fun setSelectedTab(tabIndex: Int) {
        if (isManagerOrOwner) {
            _selectedTab.value = tabIndex
        }
    }
}
