package com.example.spjappapk.ui.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.util.CurrencyUtil
import com.example.spjappapk.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPaymentsScreen(
    viewModel: AllPaymentsViewModel,
    onNavigateToPaymentDetail: (saleId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSelectSaleDialog by remember { mutableStateOf(false) }
    var selectedSaleForPayment by remember { mutableStateOf<SalesMaster?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Pembayaran") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSelectSaleDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pembayaran")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Header Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.isManagerOrOwner) "Total Pembayaran Diterima (Tim)" else "Total Pembayaran Saya",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = CurrencyUtil.formatRupiah(uiState.totalCollected),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💵 CASH: ${CurrencyUtil.formatRupiah(uiState.cashTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "🏦 TRANSFER: ${CurrencyUtil.formatRupiah(uiState.transferTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Cari Nota, Pelanggan, atau Sales...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.payments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Belum ada riwayat pembayaran." else "Pembayaran tidak ditemukan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (uiState.isManagerOrOwner) {
                    // Grouped by Sales Person for Manager
                    val grouped = remember(uiState.payments) {
                        uiState.payments.groupBy { it.salesName }.toList().sortedByDescending { (_, items) -> items.sumOf { it.payment.amount } }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(grouped) { (salesName, items) ->
                            SalesPaymentGroupCard(
                                salesName = salesName,
                                items = items,
                                onNavigateToPaymentDetail = onNavigateToPaymentDetail
                            )
                        }
                    }
                } else {
                    // Direct List for Sales
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.payments) { displayItem ->
                            PaymentDisplayCard(
                                displayItem = displayItem,
                                onNavigateToPaymentDetail = onNavigateToPaymentDetail
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Select Unpaid Invoice for new payment
    if (showSelectSaleDialog) {
        SelectUnpaidSaleDialog(
            unpaidSales = uiState.unpaidSales,
            onDismiss = { showSelectSaleDialog = false },
            onSaleSelected = { sale ->
                selectedSaleForPayment = sale
                showSelectSaleDialog = false
            }
        )
    }

    // Dialog 2: Payment Form
    if (selectedSaleForPayment != null) {
        PaymentFormDialog(
            maxAmount = selectedSaleForPayment!!.remaining,
            onDismiss = { selectedSaleForPayment = null },
            onConfirm = { amount, method, notes, date ->
                viewModel.addPayment(
                    saleId = selectedSaleForPayment!!.id,
                    amount = amount,
                    method = method,
                    notes = notes,
                    date = date
                )
                selectedSaleForPayment = null
            }
        )
    }
}

@Composable
fun SalesPaymentGroupCard(
    salesName: String,
    items: List<PaymentDisplayItem>,
    onNavigateToPaymentDetail: (saleId: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalGroup = items.sumOf { it.payment.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "👤 Sales: $salesName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Diterima: ${CurrencyUtil.formatRupiah(totalGroup)} • ${items.size} Transaksi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    items.forEach { displayItem ->
                        PaymentDisplayCard(
                            displayItem = displayItem,
                            onNavigateToPaymentDetail = onNavigateToPaymentDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDisplayCard(
    displayItem: PaymentDisplayItem,
    onNavigateToPaymentDetail: (saleId: String) -> Unit
) {
    val p = displayItem.payment

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (displayItem.sale != null) {
                    onNavigateToPaymentDetail(displayItem.sale.id)
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CurrencyUtil.formatRupiah(p.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val methodColor = if (p.paymentMethod.uppercase() == "CASH") Color(0xFF2E7D32) else Color(0xFF1565C0)
                    Badge(containerColor = methodColor.copy(alpha = 0.15f)) {
                        Text(
                            text = p.paymentMethod,
                            color = methodColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val statusText = when (p.status.uppercase()) {
                        "APPROVED" -> "✅ Disetujui"
                        "REJECTED" -> "❌ Ditolak"
                        else -> "⌛ Pending"
                    }

                    val statusColor = when (p.status.uppercase()) {
                        "APPROVED" -> Color(0xFF388E3C)
                        "REJECTED" -> Color(0xFFD32F2F)
                        else -> Color(0xFFE65100)
                    }

                    Badge(containerColor = statusColor.copy(alpha = 0.15f)) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nota: ${displayItem.nota} • ${displayItem.customerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tgl: ${DateTimeUtil.formatDisplay(p.paymentDate)} • Sales: ${displayItem.salesName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!p.isSynced) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Pending Sync",
                        tint = Color(0xFFFFA500),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!p.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Catatan: ${p.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SelectUnpaidSaleDialog(
    unpaidSales: List<SalesMaster>,
    onDismiss: () -> Unit,
    onSaleSelected: (SalesMaster) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(unpaidSales, searchQuery) {
        if (searchQuery.isBlank()) {
            unpaidSales
        } else {
            unpaidSales.filter {
                it.nota.contains(searchQuery, ignoreCase = true) ||
                (it.customerName?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (it.salesName?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Nota yang Akan Dibayar") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari Nota atau Pelanggan...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada nota dengan sisa tagihan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filtered) { sale ->
                            ListItem(
                                headlineContent = { Text(text = "Nota: ${sale.nota} • ${sale.customerName ?: "Pelanggan"}", fontWeight = FontWeight.Bold) },
                                supportingContent = {
                                    Text("Sisa Piutang: ${CurrencyUtil.formatRupiah(sale.remaining)}", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                },
                                leadingContent = {
                                    Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.clickable { onSaleSelected(sale) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
