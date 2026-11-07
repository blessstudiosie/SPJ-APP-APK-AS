package com.example.spjappapk.ui.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.util.CurrencyUtil
import com.example.spjappapk.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingOmzetScreen(
    viewModel: PendingOmzetViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedOrderForDetail by remember { mutableStateOf<OrderDisplayItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Antrean Omset (Proses)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Manager TabRow (Per Sales / Tim vs Pribadi)
            if (uiState.isManagerOrOwner) {
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        text = { Text("Per Sales (Tim)") },
                        icon = { Icon(Icons.Default.Group, null) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        text = { Text("Pribadi") },
                        icon = { Icon(Icons.Default.Person, null) }
                    )
                }
            }

            // Summary Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.isManagerOrOwner && uiState.selectedTab == 0) "Total Antrean Omset Tim" else "Total Antrean Omset Saya",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF0288D1)
                    )
                    Text(
                        text = CurrencyUtil.formatRupiah(uiState.totalPendingAmount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(containerColor = Color(0xFF0288D1)) {
                        Text("${uiState.totalPendingCount} Nota Pesanan", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
                placeholder = { Text("Cari Pelanggan, Sales, atau ID Order...") },
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

            if (uiState.orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Tidak ada antrean omset/pesanan aktif." else "Antrean pesanan tidak ditemukan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (uiState.isManagerOrOwner && uiState.selectedTab == 0) {
                    // MANAGER VIEW: Grouped per Sales Person
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.groupedOrders) { (salesName, items) ->
                            SalesPendingGroupCard(
                                salesName = salesName,
                                items = items,
                                onOrderClick = { selectedOrderForDetail = it }
                            )
                        }
                    }
                } else {
                    // DIRECT LIST VIEW: Personal or Sales role
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.orders) { displayItem ->
                            PendingOrderCard(
                                item = displayItem,
                                onClick = { selectedOrderForDetail = displayItem }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Order Details Dialog
        if (selectedOrderForDetail != null) {
            PendingOrderDetailsDialog(
                displayItem = selectedOrderForDetail!!,
                viewModel = viewModel,
                onDismiss = { selectedOrderForDetail = null }
            )
        }
    }
}

@Composable
fun SalesPendingGroupCard(
    salesName: String,
    items: List<OrderDisplayItem>,
    onOrderClick: (OrderDisplayItem) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalGroupAmount = items.sumOf { it.calculatedTotal }

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
                        text = "Total Antrean: ${CurrencyUtil.formatRupiah(totalGroupAmount)} • ${items.size} Nota",
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
                        PendingOrderCard(
                            item = displayItem,
                            onClick = { onOrderClick(displayItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingOrderCard(
    item: OrderDisplayItem,
    onClick: () -> Unit
) {
    val order = item.order
    val statusColor = when (order.status.uppercase()) {
        "SO" -> Color(0xFF0288D1)
        "ON PROSES" -> Color(0xFFE65100)
        "DALAM PENGIRIMAN" -> Color(0xFF1565C0)
        "PROCESSED" -> Color(0xFF388E3C)
        else -> Color(0xFF757575)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Badge(containerColor = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        text = order.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Order #${order.id}" + (if (!order.remoteId.isNullOrBlank()) " (${order.remoteId})" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estimasi Nominal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtil.formatRupiah(item.calculatedTotal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tgl: ${DateTimeUtil.formatDisplay(order.timestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.items.size} Produk",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PendingOrderDetailsDialog(
    displayItem: OrderDisplayItem,
    viewModel: PendingOmzetViewModel,
    onDismiss: () -> Unit
) {
    val order = displayItem.order
    var currentItems by remember(order.id, order.remoteId) { mutableStateOf(displayItem.items) }
    var isLoadingItems by remember(order.id, order.remoteId) { mutableStateOf(currentItems.isEmpty() && !order.remoteId.isNullOrBlank()) }

    LaunchedEffect(order.id, order.remoteId) {
        if (currentItems.isEmpty() && !order.remoteId.isNullOrBlank()) {
            isLoadingItems = true
            currentItems = viewModel.getOrderItems(displayItem)
            isLoadingItems = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Rincian Pesanan #${order.id}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Pelanggan: ${displayItem.customerName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Sales: ${displayItem.salesName}", style = MaterialTheme.typography.bodySmall)
                Text("Tanggal: ${DateTimeUtil.formatDisplay(order.timestamp)}", style = MaterialTheme.typography.bodySmall)
                Text("Status: ${order.status}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                if (!order.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Catatan: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Item Barang:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingItems) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (currentItems.isEmpty()) {
                    Text("Tidak ada rincian item.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    currentItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.displayProductName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} x ${CurrencyUtil.formatRupiah(item.displayPrice)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = CurrencyUtil.formatRupiah(item.displayPrice * item.quantity),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL ESTIMASI", fontWeight = FontWeight.Bold)
                    Text(CurrencyUtil.formatRupiah(displayItem.calculatedTotal), fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
