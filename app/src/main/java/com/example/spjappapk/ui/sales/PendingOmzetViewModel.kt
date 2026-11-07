package com.example.spjappapk.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.local.entities.SalesOrderItem
import com.example.spjappapk.data.local.entities.SalesPersonEntity
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.util.BusinessRevenueHelper
import com.example.spjappapk.util.DateTimeUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class OrderDisplayItem(
    val order: SalesOrder,
    val items: List<SalesOrderItem>,
    val calculatedTotal: Double,
    val salesName: String,
    val customerName: String
)

data class PendingOmzetUiState(
    val isManagerOrOwner: Boolean = false,
    val selectedTab: Int = 0, // 0: Per Sales / Tim, 1: Pribadi
    val orders: List<OrderDisplayItem> = emptyList(),
    val groupedOrders: List<Pair<String, List<OrderDisplayItem>>> = emptyList(),
    val totalPendingAmount: Double = 0.0,
    val totalPendingCount: Int = 0,
    val isLoading: Boolean = false
)

class PendingOmzetViewModel(
    private val app: SPJApplication,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val isManagerOrOwner = currentUser?.role?.trim()?.lowercase() in listOf("manager", "owner", "admin")

    val uiState: StateFlow<PendingOmzetUiState> = combine(
        app.salesRepository.allSalesMaster,
        app.salesRepository.allSales,
        app.database.salesPersonDao().getAllSalesPersons(),
        app.customerRepository.allCustomers,
        _searchQuery,
        _selectedTab
    ) { flows ->
        val masterSalesList = flows[0] as List<SalesMaster>
        val localQueueOrders = flows[1] as List<SalesOrder>
        val staffList = flows[2] as List<SalesPersonEntity>
        val customerList = flows[3] as List<Customer>
        val query = flows[4] as String
        val tab = flows[5] as Int

        val pendingStatuses = setOf("SO", "ON PROSES", "DALAM PENGIRIMAN", "PENDING", "PROCESSED")

        // 1. Convert active pending SalesMaster items from Supabase into OrderDisplayItem
        val activeMasterPending = masterSalesList.filter { sale ->
            sale.isDeleted != true && sale.status.trim().uppercase() in pendingStatuses
        }.map { sale ->
            val spName = BusinessRevenueHelper.resolveSalesPersonName(sale, staffList)
            val custName = BusinessRevenueHelper.resolveCustomerName(sale.customerName, sale.customerId, customerList)
            val orderRep = SalesOrder(
                id = sale.nota.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L,
                remoteId = sale.id,
                salesPersonId = sale.salesPersonId ?: "",
                salesPersonName = spName,
                customerId = sale.customerId ?: "",
                customerName = custName,
                orderDate = DateTimeUtil.formatDateOnly(sale.orderDate),
                totalAmount = sale.totalAmount,
                details = "[]",
                notes = sale.description,
                status = sale.status,
                timestamp = sale.orderDate
            )
            OrderDisplayItem(
                order = orderRep,
                items = emptyList(),
                calculatedTotal = sale.totalAmount,
                salesName = spName,
                customerName = custName
            )
        }

        // 2. Convert active local SalesOrder queue items into OrderDisplayItem
        val activeMasterRemoteIds = activeMasterPending.mapNotNull { it.order.remoteId }.toSet()
        val activeLocalPending = localQueueOrders.filter { order ->
            order.status.trim().uppercase() in pendingStatuses &&
            (order.remoteId.isNullOrBlank() || !activeMasterRemoteIds.contains(order.remoteId))
        }.map { order ->
            val items = try {
                json.decodeFromString<List<SalesOrderItem>>(order.details)
            } catch (e: Exception) {
                emptyList()
            }
            val calcTotal = if (order.totalAmount > 0) order.totalAmount else items.sumOf { it.displayPrice * it.quantity }
            val spName = if (order.salesPersonName.isNotBlank()) order.salesPersonName else {
                staffList.find { it.id == order.salesPersonId }?.name ?: "Sales"
            }
            val custName = BusinessRevenueHelper.resolveCustomerName(order.customerName, order.customerId, customerList)

            OrderDisplayItem(
                order = order,
                items = items,
                calculatedTotal = calcTotal,
                salesName = spName,
                customerName = custName
            )
        }

        // Combine both sources
        val allDisplayItems = (activeMasterPending + activeLocalPending).sortedByDescending { it.order.timestamp }

        // 3. Filter by Role & Selected Tab
        val roleFiltered = if (isManagerOrOwner) {
            if (tab == 1) { // Pribadi view for manager
                allDisplayItems.filter { item ->
                    item.order.salesPersonId.equals(currentUser?.id, ignoreCase = true) ||
                    item.salesName.equals(currentUser?.name, ignoreCase = true)
                }
            } else { // Per Sales / Tim view
                allDisplayItems
            }
        } else {
            // Sales Role: only personal queue
            allDisplayItems.filter { item ->
                item.order.salesPersonId.equals(currentUser?.id, ignoreCase = true) ||
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
                item.order.id.toString().contains(query, ignoreCase = true) ||
                (item.order.remoteId?.contains(query, ignoreCase = true) ?: false) ||
                item.order.status.contains(query, ignoreCase = true)
            }
        }

        // 5. Group by Sales Person for Manager Tim view
        val grouped = filtered.groupBy { it.salesName }
            .toList()
            .sortedByDescending { (_, items) -> items.sumOf { it.calculatedTotal } }

        val totalAmount = filtered.sumOf { it.calculatedTotal }

        PendingOmzetUiState(
            isManagerOrOwner = isManagerOrOwner,
            selectedTab = tab,
            orders = filtered,
            groupedOrders = grouped,
            totalPendingAmount = totalAmount,
            totalPendingCount = filtered.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PendingOmzetUiState(isLoading = true))

    suspend fun getOrderItems(displayItem: OrderDisplayItem): List<SalesOrderItem> {
        if (displayItem.items.isNotEmpty()) return displayItem.items
        val saleId = displayItem.order.remoteId ?: return emptyList()
        val details = app.salesRepository.getSalesDetails(saleId)
        val productsMap = app.inventoryRepository.allInventory.firstOrNull()?.associateBy { it.remoteId } ?: emptyMap()
        return details.map { detail ->
            val prodName = detail.itemName ?: productsMap[detail.productId]?.name ?: "Produk"
            SalesOrderItem(
                productId = detail.productId ?: "",
                itemName = prodName,
                quantity = detail.quantity,
                price = detail.price,
                priceCategory = detail.priceCategory ?: "R"
            )
        }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            if (currentUser != null) {
                app.salesRepository.refreshSalesMaster(currentUser.id, isManagerOrOwner, isFullSync = true)
            }
            app.salesRepository.scheduleSync()
        }
    }
}
