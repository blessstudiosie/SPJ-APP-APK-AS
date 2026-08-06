package com.example.spjappapk.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.local.entities.Visit
import com.example.spjappapk.data.repository.SalesRepository
import com.example.spjappapk.data.repository.VisitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PendingSyncUiState(
    val pendingOrders: List<SalesOrder> = emptyList(),
    val pendingVisits: List<Visit> = emptyList()
)

class PendingSyncViewModel(
    private val salesRepository: SalesRepository,
    private val visitRepository: VisitRepository
) : ViewModel() {

    val uiState: StateFlow<PendingSyncUiState> = combine(
        salesRepository.allSales,
        visitRepository.allVisits
    ) { sales, visits ->
        PendingSyncUiState(
            pendingOrders = sales.filter { it.status == "PENDING" },
            pendingVisits = visits.filter { it.syncStatus == "PENDING" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PendingSyncUiState())

    fun forceSync() {
        salesRepository.scheduleSync()
        visitRepository.scheduleSync()
    }
}
