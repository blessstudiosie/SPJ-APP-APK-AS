package com.example.spjappapk.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.data.local.entities.*
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.remote.models.SalesDetail
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.InventoryRepository
import com.example.spjappapk.data.repository.SalesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.jan.supabase.postgrest.postgrest

class SalesViewModel(
    private val app: SPJApplication,
    private val repository: SalesRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
    val currentUser: com.example.spjappapk.ui.auth.SalesPerson?
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val finalizedSales: StateFlow<List<SalesMaster>> = combine(
        repository.allSalesMaster,
        customerRepository.allCustomers,
        app.database.salesPersonDao().getAllSalesPersons(),
        _searchQuery
    ) { salesList, customers, salesPersons, query ->
        val resolved = salesList.map { sale ->
            val custNameFromDb = customers.find { it.remoteId == sale.customerId }?.name
            val salesNameFromDb = salesPersons.find { it.id == sale.salesPersonId }?.name
                ?: if (sale.salesPersonId == currentUser?.id) currentUser?.name else null
            
            val finalCustName = when {
                custNameFromDb != null -> custNameFromDb
                !sale.customerName.isNullOrBlank() -> sale.customerName
                else -> "Data konsumen tidak ditemukan"
            }

            val finalSalesName = (salesNameFromDb ?: sale.salesName)?.takeIf { it.isNotBlank() } ?: "Unknown Sales"

            sale.copy(
                customerName = finalCustName,
                salesName = finalSalesName
            )
        }

        if (query.isBlank()) {
            resolved
        } else {
            resolved.filter { 
                (it.customerName?.contains(query, ignoreCase = true) == true) || 
                it.nota.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRemoteLoading = MutableStateFlow(false)
    val isRemoteLoading = _isRemoteLoading.asStateFlow()

    val sales: StateFlow<Map<String, List<SalesOrder>>> = combine(
        repository.allSales,
        _searchQuery
    ) { list, query ->
        val isManagerOrOwner = currentUser?.role?.lowercase() == "manager" || currentUser?.role?.lowercase() == "owner"
        val filtered = if (isManagerOrOwner) {
            list
        } else {
            list.filter { it.salesPersonId == currentUser?.id }
        }

        val result = if (query.isBlank()) {
            filtered.filter { it.status != "APPROVED" }
        } else {
            filtered.filter { 
                it.customerName.contains(query, ignoreCase = true) || 
                it.status.contains(query, ignoreCase = true)
            }
        }
        result.sortedBy { it.timestamp }.groupBy { it.status }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val products: StateFlow<List<Inventory>> = inventoryRepository.allInventory
        .map { list -> list.filter { it.status == "Y" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchFinalizedSales()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun fetchFinalizedSales() {
        viewModelScope.launch {
            _isRemoteLoading.value = true
            try {
                val isManagerOrOwner = currentUser?.role?.lowercase() == "manager" || currentUser?.role?.lowercase() == "owner"
                repository.refreshSalesMaster(
                    currentUser?.id ?: "",
                    isManagerOrOwner
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRemoteLoading.value = false
            }
        }
    }

    fun addOrder(customerId: String, customerName: String, salesPersonName: String, orderDate: String, notes: String?, items: List<SalesOrderItem>) {
        viewModelScope.launch {
            val userId = currentUser?.id ?: "anonymous"
            val order = SalesOrder(
                salesPersonId = userId,
                salesPersonName = salesPersonName,
                customerId = customerId,
                customerName = customerName,
                orderDate = orderDate,
                notes = notes,
                details = Json.encodeToString(items),
                status = "PENDING"
            )
            repository.insertOrder(order)
        }
    }

    fun addCustomerManually(customer: Customer) {
        viewModelScope.launch {
            customerRepository.addCustomer(customer)
        }
    }

    suspend fun getSalesDetailsWithNames(salesId: String): List<SalesOrderItem> {
        val details = repository.getSalesDetails(salesId)
        val productsMap = products.value.associateBy { it.remoteId }
        
        return details.map { detail ->
            val prodName = detail.itemName ?: productsMap[detail.productId]?.name ?: "Produk tidak ditemukan"
            SalesOrderItem(
                productId = detail.productId ?: "",
                itemName = prodName,
                quantity = detail.quantity,
                price = detail.price,
                priceCategory = detail.priceCategory ?: "R"
            )
        }
    }
}
