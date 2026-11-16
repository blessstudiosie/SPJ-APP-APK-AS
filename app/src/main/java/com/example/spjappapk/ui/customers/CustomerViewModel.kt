package com.example.spjappapk.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.ui.auth.SalesPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class CustomerViewModel(
    private val repository: CustomerRepository,
    private val currentUser: SalesPerson? = null
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val customers: StateFlow<List<Customer>> = combine(
        repository.allCustomers,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { 
                it.name.contains(query, ignoreCase = true) || 
                (it.address?.contains(query, ignoreCase = true) ?: false) ||
                (it.ownerName?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val canEditLimit: Boolean
        get() {
            val r = currentUser?.role?.trim()?.lowercase() ?: ""
            return r == "manager" || r == "owner" || r == "admin" || r == "developer"
        }

    init {
        refresh()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCustomer(
        name: String,
        address: String,
        ownerName: String?,
        phone: String?,
        deliveryRoute: String?,
        creditLimit: Double,
        latitude: Double?,
        longitude: Double?
    ) {
        viewModelScope.launch {
            val newCustomer = Customer(
                remoteId = UUID.randomUUID().toString(),
                name = name,
                address = address,
                ownerName = ownerName,
                phone = phone,
                deliveryRoute = deliveryRoute ?: "LOBAR-MATARAM",
                creditLimit = if (canEditLimit) creditLimit else 0.0,
                latitude = latitude,
                longitude = longitude,
                salesPersonId = currentUser?.id,
                salesPersonName = currentUser?.name,
                syncStatus = "PENDING"
            )
            repository.addCustomer(newCustomer)
        }
    }

    fun updateCustomer(
        existing: Customer,
        name: String,
        address: String,
        ownerName: String?,
        phone: String?,
        deliveryRoute: String?,
        creditLimit: Double,
        latitude: Double?,
        longitude: Double?
    ) {
        viewModelScope.launch {
            val updated = existing.copy(
                name = name,
                address = address,
                ownerName = ownerName,
                phone = phone,
                deliveryRoute = deliveryRoute ?: existing.deliveryRoute ?: "LOBAR-MATARAM",
                creditLimit = if (canEditLimit) creditLimit else existing.creditLimit,
                latitude = latitude ?: existing.latitude,
                longitude = longitude ?: existing.longitude,
                syncStatus = "PENDING"
            )
            repository.addCustomer(updated)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshCustomers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
