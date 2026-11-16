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
            if (uiState.totalPendingCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("✅ Semua data offline sudah tersinkronisasi ke server.")
                }
            } else {
                Button(
                    onClick = { viewModel.forceSync() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔄 Kirim Semua ke Server Sekarang (${uiState.totalPendingCount} Item)")
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (uiState.pendingOrders.isNotEmpty()) {
                        item {
                            ListHeader("Pesanan SO Belum Terkirim (${uiState.pendingOrders.size})")
                        }
                        items(uiState.pendingOrders) { order ->
                            ListItem(
                                headlineContent = { Text(order.customerName.ifEmpty { "Customer ID: ${order.customerId}" }) },
                                supportingContent = { Text("Tanggal: ${order.orderDate} | Total: ${com.example.spjappapk.util.CurrencyUtil.formatRupiah(order.totalAmount)}") },
                                trailingContent = { Badge { Text("SO Pending") } }
                            )
                            HorizontalDivider()
                        }
                    }

                    if (uiState.pendingVisits.isNotEmpty()) {
                        item {
                            ListHeader("Laporan Kunjungan Belum Terkirim (${uiState.pendingVisits.size})")
                        }
                        items(uiState.pendingVisits) { visit ->
                            ListItem(
                                headlineContent = { Text(visit.customerName.ifEmpty { "Kunjungan ID: ${visit.customerId}" }) },
                                supportingContent = { Text("Lat: ${visit.locationLat}, Lng: ${visit.locationLng}") },
                                trailingContent = { Badge { Text("Pending") } }
                            )
                            HorizontalDivider()
                        }
                    }

                    if (uiState.pendingCustomers.isNotEmpty()) {
                        item {
                            ListHeader("Data Konsumen Baru/Edit Belum Terkirim (${uiState.pendingCustomers.size})")
                        }
                        items(uiState.pendingCustomers) { customer ->
                            ListItem(
                                headlineContent = { Text(customer.name) },
                                supportingContent = { Text("${customer.address ?: "-"} (${customer.deliveryRoute ?: "LOBAR-MATARAM"})") },
                                trailingContent = { Badge { Text("Pending") } }
                            )
                            HorizontalDivider()
                        }
                    }

                    if (uiState.pendingPayments.isNotEmpty()) {
                        item {
                            ListHeader("Input Pembayaran Belum Terkirim (${uiState.pendingPayments.size})")
                        }
                        items(uiState.pendingPayments) { payment ->
                            ListItem(
                                headlineContent = { Text("Pembayaran Nota: ${payment.saleId}") },
                                supportingContent = { Text("${payment.paymentMethod} - ${com.example.spjappapk.util.CurrencyUtil.formatRupiah(payment.amount)}") },
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
