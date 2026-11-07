package com.example.spjappapk.ui.visits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.SalesPersonEntity
import com.example.spjappapk.data.local.entities.Visit
import com.example.spjappapk.data.local.entities.deduplicateVisits
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.util.BusinessRevenueHelper
import com.example.spjappapk.util.DateTimeUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

data class TodayVisitDisplayItem(
    val visit: Visit,
    val customerName: String,
    val salesName: String,
    val timeFormatted: String
)

data class TodayVisitsUiState(
    val isManagerOrOwner: Boolean = false,
    val selectedTab: Int = 0, // 0: Per Sales (Tim), 1: Pribadi
    val visits: List<TodayVisitDisplayItem> = emptyList(),
    val groupedVisits: List<Pair<String, List<TodayVisitDisplayItem>>> = emptyList(),
    val totalVisitsToday: Int = 0,
    val uniqueOutletsToday: Int = 0,
    val dateText: String = "",
    val isLoading: Boolean = false
)

class TodayVisitsViewModel(
    private val app: SPJApplication,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val isManagerOrOwner = currentUser?.role?.trim()?.lowercase() in listOf("manager", "owner", "admin")

    val uiState: StateFlow<TodayVisitsUiState> = combine(
        app.visitRepository.allVisits,
        app.database.salesPersonDao().getAllSalesPersons(),
        app.customerRepository.allCustomers,
        _searchQuery,
        _selectedTab
    ) { flows ->
        val allVisits = flows[0] as List<Visit>
        val staffList = flows[1] as List<SalesPersonEntity>
        val customerList = flows[2] as List<Customer>
        val query = flows[3] as String
        val tab = flows[4] as Int

        val startOfDay = DateTimeUtil.getStartOfDay()
        val endOfDay = DateTimeUtil.getEndOfDay()
        val dateText = DateTimeUtil.formatDateOnly(startOfDay)

        // 1. Deduplicate & filter today's visits
        val deduplicated = allVisits.deduplicateVisits()
        val todayVisits = deduplicated.filter { it.timestamp in startOfDay..endOfDay }

        // 2. Build display items
        val displayItems = todayVisits.map { visit ->
            val custName = BusinessRevenueHelper.resolveCustomerName(visit.customerName, visit.customerId, customerList)
            val spName = if (visit.salesPersonName.isNotBlank()) visit.salesPersonName else {
                staffList.find { it.id == visit.userId }?.name ?: "Sales"
            }
            val timeStr = DateTimeUtil.getFormat("HH:mm").format(Date(visit.timestamp)) + " WITA"

            TodayVisitDisplayItem(
                visit = visit,
                customerName = custName,
                salesName = spName,
                timeFormatted = timeStr
            )
        }

        // 3. Filter by Role & Selected Tab
        val roleFiltered = if (isManagerOrOwner) {
            if (tab == 1) { // Pribadi view for manager
                displayItems.filter { item ->
                    item.visit.userId.equals(currentUser?.id, ignoreCase = true) ||
                    item.salesName.equals(currentUser?.name, ignoreCase = true)
                }
            } else { // Per Sales / Tim view
                displayItems
            }
        } else {
            // Sales Role: only personal visits
            displayItems.filter { item ->
                item.visit.userId.equals(currentUser?.id, ignoreCase = true) ||
                item.salesName.equals(currentUser?.name, ignoreCase = true)
            }
        }

        // 4. Search Filter
        val filtered = if (query.isBlank()) {
            roleFiltered
        } else {
            roleFiltered.filter { item ->
                item.customerName.contains(query, ignoreCase = true) ||
                item.salesName.contains(query, ignoreCase = true) ||
                (item.visit.notes?.contains(query, ignoreCase = true) ?: false)
            }
        }

        // 5. Group by Sales Person for Manager Tim view
        val grouped = filtered.groupBy { it.salesName }
            .toList()
            .sortedByDescending { (_, items) -> items.size }

        val uniqueOutlets = filtered.map { it.visit.customerId }.distinct().size

        TodayVisitsUiState(
            isManagerOrOwner = isManagerOrOwner,
            selectedTab = tab,
            visits = filtered,
            groupedVisits = grouped,
            totalVisitsToday = filtered.size,
            uniqueOutletsToday = uniqueOutlets,
            dateText = dateText,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayVisitsUiState(isLoading = true))

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            currentUser?.let { app.visitRepository.refreshVisits(it.id) }
        }
    }
}
