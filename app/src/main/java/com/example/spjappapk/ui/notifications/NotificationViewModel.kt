package com.example.spjappapk.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.repository.SalesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NotificationViewModel(private val salesRepository: SalesRepository) : ViewModel() {

    val notifications: StateFlow<List<SalesOrder>> = salesRepository.allSales
        .map { list -> 
            list.filter { it.status != "PENDING" && it.status != "PROCESSED" }
                .sortedByDescending { it.timestamp }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
