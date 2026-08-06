package com.example.spjappapk.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingSyncScreen(viewModel: PendingSyncViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Antrean Sinkronisasi") },
                actions = {
                    IconButton(onClick = { viewModel.forceSync() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Now")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.pendingOrders.isEmpty() && uiState.pendingVisits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Semua data sudah tersinkronisasi.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (uiState.pendingOrders.isNotEmpty()) {
                        item {
                            ListHeader("Pesanan Belum Terkirim (${uiState.pendingOrders.size})")
                        }
                        items(uiState.pendingOrders) { order ->
                            ListItem(
                                headlineContent = { Text("Order untuk Customer ID: ${order.customerId}") },
                                supportingContent = { Text("Tanggal: ${order.orderDate}") },
                                trailingContent = { Badge { Text("Pending") } }
                            )
                            HorizontalDivider()
                        }
                    }

                    if (uiState.pendingVisits.isNotEmpty()) {
                        item {
                            ListHeader("Kunjungan Belum Terkirim (${uiState.pendingVisits.size})")
                        }
                        items(uiState.pendingVisits) { visit ->
                            ListItem(
                                headlineContent = { Text("Kunjungan Toko ID: ${visit.customerId}") },
                                supportingContent = { Text("Lat: ${visit.locationLat}, Lng: ${visit.locationLng}") },
                                trailingContent = { Badge { Text("Pending") } }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListHeader(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp, 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
