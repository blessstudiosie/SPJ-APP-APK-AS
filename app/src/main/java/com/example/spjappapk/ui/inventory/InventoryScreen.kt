package com.example.spjappapk.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel) {
    val items by viewModel.inventoryItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var selectedItem by remember { mutableStateOf<Inventory?>(null) }

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
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onRefresh = { viewModel.refresh() },
            onItemClick = { item -> selectedItem = item }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryList(
    items: List<Inventory>,
    isRefreshing: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (Inventory) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inventory (Stok)") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (items.isEmpty() && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (searchQuery.isEmpty()) "Data produk belum tersedia" else "Produk tidak ditemukan")
                            if (searchQuery.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
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
                                headlineContent = { Text(item.name) },
                                supportingContent = { 
                                    Column {
                                        Text("Kategori: ${item.category ?: "Umum"}")
                                        Text("Stok: ${item.stockQuantity} ${item.unit ?: ""}") 
                                    }
                                },
                                trailingContent = { 
                                    Text("Rp ${String.format(Locale.getDefault(), "%,.0f", item.priceR)}") 
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
            
            DetailRow("Harga Retail (R)", "Rp ${String.format(Locale.getDefault(), "%,.0f", item.priceR)}")
            DetailRow("Harga Semi Grosir (SG)", "Rp ${String.format(Locale.getDefault(), "%,.0f", item.priceSG)}")
            DetailRow("Harga Grosir (G)", "Rp ${String.format(Locale.getDefault(), "%,.0f", item.priceG)}")
            DetailRow("Harga Partai (P)", "Rp ${String.format(Locale.getDefault(), "%,.0f", item.priceP)}")
            
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
