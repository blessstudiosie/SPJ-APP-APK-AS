package com.example.spjappapk.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.local.entities.SalesOrderItem
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.InventoryRepository
import com.example.spjappapk.data.repository.SalesRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SalesViewModel(
    private val repository: SalesRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {
    val sales: StateFlow<List<SalesOrder>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Inventory>> = inventoryRepository.allInventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addOrder(customerId: String, orderDate: String, notes: String?, items: List<SalesOrderItem>) {
        viewModelScope.launch {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "anonymous"
            val order = SalesOrder(
                salesPersonId = userId,
                customerId = customerId,
                orderDate = orderDate,
                notes = notes,
                details = Json.encodeToString(items),
                status = "PENDING"
            )
            repository.insertOrder(order)
        }
    }
}
