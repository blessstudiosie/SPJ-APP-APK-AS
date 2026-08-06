package com.example.spjappapk.ui.visits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.Visit
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.VisitRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class VisitsViewModel(
    private val repository: VisitRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {
    val visits: StateFlow<List<Visit>> = repository.allVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addVisit(customerId: String, lat: Double, lng: Double, photoPath: String?) {
        viewModelScope.launch {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "anonymous"
            val visit = Visit(
                userId = userId,
                customerId = customerId,
                locationLat = lat,
                locationLng = lng,
                photoPath = photoPath,
                syncStatus = "PENDING"
            )
            repository.insertVisit(visit)
        }
    }

    fun createCustomer(name: String, address: String?) {
        viewModelScope.launch {
            val newCustomer = Customer(
                remoteId = UUID.randomUUID().toString(),
                name = name,
                address = address
            )
            customerRepository.addCustomer(newCustomer)
        }
    }
}
