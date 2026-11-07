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

import kotlinx.coroutines.flow.map

import com.example.spjappapk.data.local.entities.deduplicateVisits

class VisitsViewModel(
    private val repository: VisitRepository,
    private val customerRepository: CustomerRepository,
    private val currentUser: com.example.spjappapk.ui.auth.SalesPerson?
) : ViewModel() {
    val visits: StateFlow<List<Visit>> = repository.allVisits
        .map { list -> list.deduplicateVisits() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addVisit(
        customerId: String,
        customerName: String,
        salesPersonName: String,
        isNewCustomer: Boolean,
        lat: Double,
        lng: Double,
        photoPath: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val visit = Visit(
                userId = currentUser?.id ?: "anonymous",
                salesPersonName = salesPersonName,
                customerId = customerId,
                customerName = customerName,
                isNewCustomer = isNewCustomer,
                locationLat = lat,
                locationLng = lng,
                photoPath = photoPath,
                notes = notes,
                syncStatus = "PENDING"
            )
            repository.insertVisit(visit)
        }
    }

    suspend fun getTodayVisitForCustomer(customerId: String): Visit? {
        return repository.getTodayVisitForCustomer(customerId)
    }

    fun updateCustomerCoordinates(customerId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            customerRepository.updateCustomerCoordinates(customerId, lat, lng)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            currentUser?.let { repository.refreshVisits(it.id) }
        }
    }

    fun createCustomer(name: String, address: String?, ownerName: String?, phone: String?) {
        viewModelScope.launch {
            val newCustomer = Customer(
                remoteId = UUID.randomUUID().toString(),
                name = name,
                address = address,
                ownerName = ownerName,
                phone = phone,
                salesPersonId = currentUser?.id,
                salesPersonName = currentUser?.name
            )
            customerRepository.addCustomer(newCustomer)
        }
    }

    fun deleteVisit(visitId: Long) {
        viewModelScope.launch {
            // Check if it's already synced? User might still want to delete local copy
            // but usually we delete pending ones.
            repository.deleteVisit(visitId)
        }
    }

    fun updateVisitNotes(visitId: Long, newNotes: String) {
        viewModelScope.launch {
            repository.updateVisitNotes(visitId, newNotes)
        }
    }
}
