package com.example.spjappapk.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(ProductFilterState())
    val filterState = _filterState.asStateFlow()

    // Dynamically extracted categories from local database
    val categories: StateFlow<List<String>> = repository.allInventory.map { list ->
        val active = list.filter { it.status == "Y" }
        listOf("Semua") + active.mapNotNull { it.category?.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Semua"))

    val inventoryItems: StateFlow<List<Inventory>> = combine(
        repository.allInventory,
        _searchQuery,
        _filterState
    ) { list, query, filter ->
        var result = list.filter { it.status == "Y" }

        // 1. Search Query Filter
        if (query.isNotBlank()) {
            result = result.filter { 
                it.name.contains(query, ignoreCase = true) || 
                (it.category?.contains(query, ignoreCase = true) ?: false)
            }
        }

        // 2. Category Filter
        if (!filter.selectedCategory.isNullOrBlank() && filter.selectedCategory != "Semua") {
            result = result.filter { it.category?.trim()?.equals(filter.selectedCategory.trim(), ignoreCase = true) == true }
        }

        // 3. Stock Ready Filter
        if (filter.onlyStockReady) {
            result = result.filter { it.stockQuantity > 0 }
        }

        // 4. Price Range Filter
        if (filter.minPrice != null) {
            result = result.filter { it.priceR >= filter.minPrice }
        }
        if (filter.maxPrice != null) {
            result = result.filter { it.priceR <= filter.maxPrice }
        }

        // 5. Sorting Options
        result = when (filter.sortBy) {
            SortOption.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
            SortOption.PRICE_LOW_HIGH -> result.sortedBy { it.priceR }
            SortOption.PRICE_HIGH_LOW -> result.sortedByDescending { it.priceR }
            SortOption.STOCK_HIGH_LOW -> result.sortedByDescending { it.stockQuantity }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterState(newState: ProductFilterState) {
        _filterState.value = newState
    }

    fun selectCategory(category: String?) {
        val cat = if (category == "Semua") null else category
        _filterState.value = _filterState.value.copy(selectedCategory = cat)
    }

    fun toggleOnlyStockReady() {
        _filterState.value = _filterState.value.copy(onlyStockReady = !_filterState.value.onlyStockReady)
    }

    fun resetFilters() {
        _filterState.value = ProductFilterState()
        _searchQuery.value = ""
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshInventory()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
