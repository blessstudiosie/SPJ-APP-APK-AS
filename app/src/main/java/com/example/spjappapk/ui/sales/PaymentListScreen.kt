package com.example.spjappapk.ui.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.entities.PaymentEntity
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.util.CurrencyUtil
import com.example.spjappapk.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentListScreen(
    viewModel: PaymentViewModel,
    onNavigateBack: () -> Unit
) {
    val sale by viewModel.sale.collectAsState()
    val items by viewModel.items.collectAsState()
    val payments by viewModel.payments.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Penjualan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1 && sale != null && sale!!.remaining > 0) {
                FloatingActionButton(onClick = { showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Pembayaran")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            sale?.let { s ->
                SaleHeader(sale = s)
            }
            
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Detail Barang") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riwayat Pembayaran") }
                )
            }

            if (selectedTab == 0) {
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (sale != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ReceiptContent(sale = sale!!, items = items)
                    }
                }
            } else {
                if (payments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada riwayat pembayaran", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(payments) { payment ->
                            PaymentItem(payment = payment)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showForm && sale != null) {
        PaymentFormDialog(
            maxAmount = sale!!.remaining,
            onDismiss = { showForm = false },
            onConfirm = { amount: Double, method: String, notes: String?, date: Long ->
                viewModel.addPayment(amount, method, notes, date)
                showForm = false
            }
        )
    }
}

@Composable
fun SaleHeader(sale: SalesMaster) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = sale.customerName ?: "Pelanggan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Nota: ${sale.nota}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Total Tagihan", style = MaterialTheme.typography.labelSmall)
                    Text(text = CurrencyUtil.formatRupiah(sale.totalAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Sisa Piutang", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = CurrencyUtil.formatRupiah(sale.remaining), 
                        style = MaterialTheme.typography.bodyLarge, 
                        fontWeight = FontWeight.Bold,
                        color = if (sale.remaining > 0) MaterialTheme.colorScheme.error else Color(0xFF388E3C)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentItem(payment: PaymentEntity) {
    val statusText = when (payment.status.uppercase()) {
        "APPROVED" -> "✅ Disetujui Admin"
        "REJECTED" -> "❌ Ditolak"
        else -> "⌛ Menunggu Konfirmasi"
    }

    val statusColor = when (payment.status.uppercase()) {
        "APPROVED" -> Color(0xFF388E3C)
        "REJECTED" -> Color(0xFFD32F2F)
        else -> Color(0xFFE65100)
    }

    ListItem(
        headlineContent = { Text(text = CurrencyUtil.formatRupiah(payment.amount), fontWeight = FontWeight.Bold) },
        supportingContent = {
            Column {
                Text(text = "Tanggal: ${DateTimeUtil.formatDisplay(payment.paymentDate)}")
                Text(text = "Metode: ${payment.paymentMethod}")
                if (!payment.notes.isNullOrBlank()) {
                    Text(text = "Catatan: ${payment.notes}")
                }
            }
        },
        leadingContent = {
            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Badge(containerColor = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (!payment.isSynced) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync, 
                            contentDescription = "Pending Sync",
                            tint = Color(0xFFFFA500),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Belum Sync", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA500))
                    }
                }
            }
        }
    )
}
