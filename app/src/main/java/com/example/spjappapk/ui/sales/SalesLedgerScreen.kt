package com.example.spjappapk.ui.sales

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.spjappapk.data.local.entities.*
import com.example.spjappapk.ui.theme.SPJAPPAPKTheme
import com.example.spjappapk.util.CurrencyUtil
import com.example.spjappapk.util.DateTimeUtil
import com.example.spjappapk.util.ImageUtils
import kotlinx.serialization.json.Json
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesLedgerScreen(
    viewModel: SalesViewModel, 
    currentSalesPersonName: String,
    onNavigateToAddOrder: () -> Unit,
    onNavigateToPayments: (String) -> Unit
) {
    val salesGrouped by viewModel.sales.collectAsState()
    val finalizedSales by viewModel.finalizedSales.collectAsState()
    val isRemoteLoading by viewModel.isRemoteLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var selectedStatusFilter by remember { mutableStateOf("SEMUA") }
    var selectedSaleForReceipt by remember { mutableStateOf<SalesMaster?>(null) }
    var selectedSaleItems by remember { mutableStateOf<List<SalesOrderItem>?>(null) }

    // Gabungkan order lokal yang belum sempat ter-pull ke SalesMaster ke dalam 1 daftar utama berstatus SO
    val unifiedSales = remember(finalizedSales, salesGrouped) {
        val existingIds = finalizedSales.map { it.id.lowercase() }.toSet()
        val localPendingAsMaster = salesGrouped.values.flatten()
            .filter { order ->
                val rid = order.remoteId?.lowercase()
                rid == null || rid !in existingIds
            }
            .map { order ->
                val rawNotes = order.notes ?: ""
                val notaLabel = rawNotes.substringBefore(" | ").trim()
                    .takeIf { it.startsWith("SO-", ignoreCase = true) }
                    ?: "SO-LOKAL"
                SalesMaster(
                    id = order.remoteId ?: "local-${order.id}",
                    nota = notaLabel,
                    orderDate = order.timestamp,
                    customerId = order.customerId,
                    customerName = order.customerName,
                    totalAmount = order.totalAmount,
                    salesPersonId = order.salesPersonId,
                    salesName = order.salesPersonName,
                    status = "SO"
                )
            }
        (localPendingAsMaster + finalizedSales).sortedByDescending { it.orderDate }
    }

    val filteredSales = remember(unifiedSales, selectedStatusFilter) {
        if (selectedStatusFilter == "SEMUA") {
            unifiedSales
        } else {
            unifiedSales.filter { it.status.trim().equals(selectedStatusFilter, ignoreCase = true) }
        }
    }

    LaunchedEffect(selectedSaleForReceipt?.id) {
        val currentSale = selectedSaleForReceipt
        if (currentSale != null) {
            selectedSaleItems = null
            if (!currentSale.id.startsWith("local-") && currentSale.nota != "PENDING") {
                selectedSaleItems = viewModel.getSalesDetailsWithNames(currentSale.id)
            } else {
                val localOrder = salesGrouped.values.flatten().find {
                    ("local-${it.id}" == currentSale.id) || (it.remoteId == currentSale.id)
                }
                if (localOrder != null) {
                    selectedSaleItems = try {
                        Json.decodeFromString<List<SalesOrderItem>>(localOrder.details)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
        } else {
            selectedSaleItems = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Penjualan") },
                actions = {
                    IconButton(onClick = { viewModel.fetchFinalizedSales() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Remote")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddOrder,
                icon = { Icon(Icons.Default.Add, contentDescription = "Tambah Pesanan") },
                text = { Text("Tambah Pesanan") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cari No Nota, Pelanggan, atau Sales...") },
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

            // Filter Status Nota (Semua penjualan di 1 tempat dipisahkan berdasarkan status nota)
            val statusOptions = listOf("SEMUA", "SO", "ON PROSES", "DALAM PENGIRIMAN", "TEMPO", "DONE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { st ->
                    val count = if (st == "SEMUA") unifiedSales.size else unifiedSales.count { it.status.trim().equals(st, ignoreCase = true) }
                    val label = if (st == "SEMUA") "Semua ($count)" else "$st ($count)"
                    FilterChip(
                        selected = selectedStatusFilter == st,
                        onClick = { selectedStatusFilter = st },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Direct Unified Sales List
            if (isRemoteLoading && unifiedSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedStatusFilter == "SEMUA") "Tidak ada data penjualan"
                        else "Tidak ada nota berstatus $selectedStatusFilter"
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredSales) { sale ->
                        RemoteSaleItem(
                            sale = sale,
                            onDownloadClick = {
                                selectedSaleItems = null
                                selectedSaleForReceipt = it
                            },
                            onClick = { onNavigateToPayments(it.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        // Receipt Dialog
        if (selectedSaleForReceipt != null) {
            ReceiptDialog(
                sale = selectedSaleForReceipt!!,
                items = selectedSaleItems,
                onDismiss = { 
                    selectedSaleForReceipt = null 
                    selectedSaleItems = null
                }
            )
        }
    }
}

@Composable
fun RemoteSaleItem(sale: SalesMaster, onDownloadClick: (SalesMaster) -> Unit, onClick: (SalesMaster) -> Unit) {
    ListItem(
        headlineContent = { Text(sale.customerName ?: "Pelanggan Tanpa Nama", fontWeight = FontWeight.Bold) },
        supportingContent = {
            Column {
                Text("Nota: ${sale.nota}")
                Text("Tgl: ${DateTimeUtil.formatDateOnly(sale.orderDate)}")
                Text("Sales: ${sale.salesName}", style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        CurrencyUtil.formatRupiah(sale.totalAmount),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (sale.status.isNotEmpty()) {
                        Text(sale.status, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onDownloadClick(sale) }) {
                    Icon(Icons.Default.Download, contentDescription = "Download Nota", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        modifier = Modifier.clickable { onClick(sale) }
    )
}

@Composable
fun SaleItem(order: SalesOrder, onClick: (SalesOrder) -> Unit) {
    val dateText = DateTimeUtil.formatDisplay(order.timestamp)
    
    ListItem(
        headlineContent = { Text(order.customerName.ifEmpty { "Order #${order.id}" }) },
        supportingContent = { 
            Column {
                if (order.customerName.isNotEmpty()) {
                    Text("Order #${order.id}")
                }
                Text("Tanggal: $dateText")
                if (!order.notes.isNullOrEmpty()) {
                    Text("Notes: ${order.notes}", style = MaterialTheme.typography.bodySmall)
                }
                if (!order.adminNotes.isNullOrEmpty()) {
                    Text("Feedback: ${order.adminNotes}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        trailingContent = { 
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(
                    containerColor = when (order.status.uppercase()) {
                        "SO" -> Color(0xFFDBEAFE)
                        "APPROVED" -> Color(0xFFDCFCE7)
                        "REJECTED" -> Color(0xFFFEE2E2)
                        else -> Color(0xFFF1F5F9)
                    },
                    contentColor = when (order.status.uppercase()) {
                        "SO" -> Color(0xFF1D4ED8)
                        "APPROVED" -> Color(0xFF15803D)
                        "REJECTED" -> Color(0xFFB91C1C)
                        else -> Color(0xFF475569)
                    }
                ) {
                    Text(
                        text = if (order.status == "PENDING" || order.status == "PROCESSED") "SO" else order.status,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                when {
                    order.remoteId != null && (order.status == "SO" || order.status == "PROCESSED") ->
                        Icon(Icons.Default.CloudDone, contentDescription = "Synced SO", tint = MaterialTheme.colorScheme.primary)
                    order.status == "APPROVED" ->
                        Icon(Icons.Default.CheckCircle, contentDescription = "Approved", tint = Color(0xFF388E3C))
                    order.status == "REJECTED" ->
                        Icon(Icons.Default.Cancel, contentDescription = "Rejected", tint = MaterialTheme.colorScheme.error)
                    else ->
                        Icon(Icons.Default.CloudQueue, contentDescription = "Pending Sync", tint = Color.Gray)
                }
            }
        },
        modifier = Modifier.clickable { onClick(order) }
    )
}

@Composable
fun ReceiptDialog(sale: SalesMaster, items: List<SalesOrderItem>?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var composeView: ComposeView? by remember { mutableStateOf(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                composeView?.let { view ->
                    val bitmap = ImageUtils.createBitmapFromView(view)
                    val uri = ImageUtils.saveBitmapToGallery(context, bitmap, "Nota_${sale.nota}")
                    if (uri != null) {
                        Toast.makeText(context, "Nota disimpan ke galeri", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal menyimpan nota", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            }) {
                Text("Download Gambar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        title = { Text("Detail Nota") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (items == null && sale.nota != "PENDING") {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ReceiptContent(sale = sale, items = items ?: emptyList())
                    
                    Box(modifier = Modifier.size(1.dp).alpha(0f)) { 
                       AndroidView(
                            factory = { ctx ->
                                ComposeView(ctx).apply {
                                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                                    setContent {
                                        SPJAPPAPKTheme {
                                            ReceiptContent(sale = sale, items = items ?: emptyList())
                                        }
                                    }
                                    composeView = this
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ReceiptContent(sale: SalesMaster, items: List<SalesOrderItem>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SARANA PRIMA JAYA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("NOTA PENJUALAN", style = MaterialTheme.typography.titleMedium, color = Color.Black)
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Nota: ${sale.nota}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                Text("Tgl: ${DateTimeUtil.formatDateOnly(sale.orderDate)}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pelanggan:", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                Text(sale.customerName ?: "-", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sales:", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                Text(sale.salesName ?: "-", style = MaterialTheme.typography.bodySmall, color = Color.Black)
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.Black)
            
            items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.displayProductName, style = MaterialTheme.typography.bodySmall, color = Color.Black)
                        Text("${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} x ${CurrencyUtil.formatRupiah(item.displayPrice)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Text(CurrencyUtil.formatRupiah(item.displayPrice * item.quantity), style = MaterialTheme.typography.bodySmall, color = Color.Black)
                }
            }
            
            HorizontalDivider(color = Color.Black)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", fontWeight = FontWeight.Bold, color = Color.Black)
                Text(CurrencyUtil.formatRupiah(sale.totalAmount), fontWeight = FontWeight.Bold, color = Color.Black)
            }
            HorizontalDivider(color = Color.Black)
            
            Spacer(Modifier.height(16.dp))
            Text("Status: ${sale.status}", style = MaterialTheme.typography.labelLarge, color = Color.Black)
            if (sale.deliveryDate != null) {
                Text("Tgl Kirim: ${sale.deliveryDate}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
            }
            Spacer(Modifier.height(16.dp))
            Text("Terima Kasih", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
