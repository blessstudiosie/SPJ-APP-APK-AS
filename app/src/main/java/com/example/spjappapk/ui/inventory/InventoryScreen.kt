package com.example.spjappapk.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.util.CurrencyUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel) {
    val items by viewModel.inventoryItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedItem by remember { mutableStateOf<Inventory?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (selectedItem != null) {
        BackHandler {
            selectedItem = null
        }
        InventoryDetail(
            item = selectedItem!!,
            onBack = { selectedItem = null }
        )
    } else {
        InventoryList(
            items = items,
            isRefreshing = isRefreshing,
            searchQuery = searchQuery,
            filterState = filterState,
            categories = categories,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onOpenFilter = { showFilterSheet = true },
            onCategorySelected = { viewModel.selectCategory(it) },
            onResetFilters = { viewModel.resetFilters() },
            onUpdateFilter = { viewModel.updateFilterState(it) },
            onRefresh = { viewModel.refresh() },
            onItemClick = { item -> selectedItem = item }
        )
    }

    if (showFilterSheet) {
        ProductFilterBottomSheet(
            initialFilterState = filterState,
            categories = categories,
            onDismiss = { showFilterSheet = false },
            onApplyFilter = { newState -> viewModel.updateFilterState(newState) },
            onResetFilter = { viewModel.resetFilters() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryList(
    items: List<Inventory>,
    isRefreshing: Boolean,
    searchQuery: String,
    filterState: ProductFilterState,
    categories: List<String>,
    onSearchQueryChange: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onUpdateFilter: (ProductFilterState) -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (Inventory) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inventory (Stok)") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar & Custom Filter Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Cari Nama atau Kategori...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                BadgedBox(
                    badge = {
                        if (filterState.isFilterActive) {
                            Badge { Text("${filterState.activeFilterCount}") }
                        }
                    }
                ) {
                    FilledTonalIconButton(onClick = onOpenFilter) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            }

            // Quick Category Chips
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = (filterState.selectedCategory == null && cat == "Semua") ||
                                filterState.selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelected(cat) },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Active Filter Summary Chips
            if (filterState.isFilterActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (filterState.onlyStockReady) {
                        AssistChip(
                            onClick = { onUpdateFilter(filterState.copy(onlyStockReady = false)) },
                            label = { Text("Stok > 0 ✕") }
                        )
                    }
                    if (filterState.minPrice != null) {
                        AssistChip(
                            onClick = { onUpdateFilter(filterState.copy(minPrice = null)) },
                            label = { Text("Min: ${CurrencyUtil.formatRupiah(filterState.minPrice)} ✕") }
                        )
                    }
                    if (filterState.maxPrice != null) {
                        AssistChip(
                            onClick = { onUpdateFilter(filterState.copy(maxPrice = null)) },
                            label = { Text("Maks: ${CurrencyUtil.formatRupiah(filterState.maxPrice)} ✕") }
                        )
                    }
                    if (filterState.sortBy != SortOption.NAME_ASC) {
                        AssistChip(
                            onClick = { onUpdateFilter(filterState.copy(sortBy = SortOption.NAME_ASC)) },
                            label = { Text("Urut: ${filterState.sortBy.label} ✕") }
                        )
                    }

                    TextButton(onClick = onResetFilters) {
                        Text("Reset Semua", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (items.isEmpty() && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isEmpty() && !filterState.isFilterActive) 
                                    "Data produk belum tersedia" 
                                else 
                                    "Produk tidak ditemukan sesuai filter",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (filterState.isFilterActive) {
                                OutlinedButton(onClick = onResetFilters) {
                                    Text("Reset Filter & Pencarian")
                                }
                            } else if (searchQuery.isEmpty()) {
                                Button(onClick = onRefresh) {
                                    Text("Tarik Data Sekarang")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items) { item ->
                            ListItem(
                                headlineContent = { Text(item.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = {
                                    Column {
                                        Text("Kategori: ${item.category ?: "Umum"}")
                                        Text(
                                            text = "Stok: ${item.stockQuantity} ${item.unit ?: ""}",
                                            color = if (item.stockQuantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            fontWeight = if (item.stockQuantity > 0) FontWeight.Normal else FontWeight.Bold
                                        )
                                    }
                                },
                                trailingContent = {
                                    Text(
                                        text = CurrencyUtil.formatRupiah(item.priceR),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.clickable { onItemClick(item) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetail(item: Inventory, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = item.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = item.category ?: "Umum", 
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            DetailRow("Stok Ready", "${item.stockQuantity} ${item.unit ?: "PCS"}")
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Keterangan Produk:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = if (item.description.isNullOrBlank()) "Tidak ada keterangan tambahan." else item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.description.isNullOrBlank()) Color.Gray else Color.Unspecified
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Daftar Harga:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            DetailRow("Harga Retail (R)", CurrencyUtil.formatRupiah(item.priceR))
            DetailRow("Harga Semi Grosir (SG)", CurrencyUtil.formatRupiah(item.priceSG))
            DetailRow("Harga Grosir (G)", CurrencyUtil.formatRupiah(item.priceG))
            DetailRow("Harga Partai (P)", CurrencyUtil.formatRupiah(item.priceP))
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "ID: ${item.remoteId}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
