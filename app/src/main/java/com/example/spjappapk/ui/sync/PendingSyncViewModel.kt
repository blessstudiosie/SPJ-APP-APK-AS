package com.example.spjappapk.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.PaymentEntity
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.local.entities.Visit
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.PaymentRepository
import com.example.spjappapk.data.repository.SalesRepository
import com.example.spjappapk.data.repository.VisitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PendingSyncUiState(
    val pendingOrders: List<SalesOrder> = emptyList(),
    val pendingVisits: List<Visit> = emptyList(),
    val pendingCustomers: List<Customer> = emptyList(),
    val pendingPayments: List<PaymentEntity> = emptyList()
) {
    val totalPendingCount: Int
        get() = pendingOrders.size + pendingVisits.size + pendingCustomers.size + pendingPayments.size
}

class PendingSyncViewModel(
    private val salesRepository: SalesRepository,
    private val visitRepository: VisitRepository,
    private val customerRepository: CustomerRepository? = null,
    private val paymentRepository: PaymentRepository? = null
) : ViewModel() {

    val uiState: StateFlow<PendingSyncUiState> = combine(
        salesRepository.allSales,
        visitRepository.allVisits,
        customerRepository?.allCustomers ?: kotlinx.coroutines.flow.flowOf(emptyList()),
        paymentRepository?.allPayments ?: kotlinx.coroutines.flow.flowOf(emptyList())
    ) { sales, visits, customers, payments ->
        PendingSyncUiState(
            pendingOrders = sales.filter { it.remoteId == null && (it.status == "PENDING" || it.status == "SO") },
            pendingVisits = visits.filter { it.syncStatus == "PENDING" },
            pendingCustomers = customers.filter { it.syncStatus == "PENDING" },
            pendingPayments = payments.filter { !it.isSynced }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PendingSyncUiState())

    fun forceSync() {
        salesRepository.scheduleSync()
        visitRepository.scheduleSync()
        customerRepository?.scheduleSync()
        paymentRepository?.scheduleSync()
    }
}
