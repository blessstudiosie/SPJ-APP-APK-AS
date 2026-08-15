package com.example.spjappapk.ui.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.spjappapk.data.local.entities.*
import com.example.spjappapk.ui.theme.SPJAPPAPKTheme
import com.example.spjappapk.util.ImageUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SalesLedgerScreen(
    viewModel: SalesViewModel, 
    currentSalesPersonName: String,
    onNavigateToAddOrder: () -> Unit
) {
    val salesGrouped by viewModel.sales.collectAsState()
    val finalizedSales by viewModel.finalizedSales.collectAsState()
    val isRemoteLoading by viewModel.isRemoteLoading.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSaleForReceipt by remember { mutableStateOf<SalesMaster?>(null) }
    var selectedSaleItems by remember { mutableStateOf<List<SalesOrderItem>?>(null) }

    LaunchedEffect(selectedSaleForReceipt) {
        if (selectedSaleForReceipt != null && selectedSaleForReceipt!!.nota != "PENDING" && selectedSaleItems == null) {
            selectedSaleItems = viewModel.getSalesDetailsWithNames(selectedSaleForReceipt!!.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Penjualan") },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = { viewModel.fetchFinalizedSales() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Remote")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddOrder) {
                Icon(Icons.Default.Add, contentDescription = "Add Order")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Antrean (Queue)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Final (Master)") }
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(if (selectedTab == 0) "Cari di antrean..." else "Cari penjualan final...") },
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

            if (selectedTab == 0) {
                if (salesGrouped.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada antrean pesanan aktif")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        salesGrouped.forEach { (status, orders) ->
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Status: $status",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            items(orders) { order ->
                                SaleItem(
                                    order = order,
                                    onClick = { 
                                        selectedSaleForReceipt = SalesMaster(
                                            id = order.remoteId ?: "local-${order.id}",
                                            nota = order.remoteId ?: "PENDING",
                                            orderDate = order.timestamp,
                                            customerName = order.customerName,
                                            totalAmount = order.totalAmount,
                                            salesName = order.salesPersonName,
                                            status = order.status
                                        )
                                        try {
                                            selectedSaleItems = kotlinx.serialization.json.Json.decodeFromString<List<SalesOrderItem>>(order.details)
                                        } catch (e: Exception) {
                                            selectedSaleItems = emptyList()
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            } else {
                if (isRemoteLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(finalizedSales) { sale ->
                            RemoteSaleItem(
                                sale = sale,
                                onDownloadClick = { selectedSaleForReceipt = it },
                                onClick = { selectedSaleForReceipt = it }
                            )
                            HorizontalDivider()
                        }
                        if (finalizedSales.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillParentMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada data penjualan final")
                                }
                            }
                        }
                    }
                }
            }
        }

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
        headlineContent = { Text(sale.customerName ?: "Pelanggan Tanpa Nama") },
        supportingContent = {
            Column {
                Text("Nota: ${sale.nota}")
                Text("Tgl: ${com.example.spjappapk.util.DateTimeUtil.formatDateOnly(sale.orderDate)}")
                Text("Sales: ${sale.salesName}", style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Rp${String.format(Locale.getDefault(), "%,.0f", sale.totalAmount)}",
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
    val dateText = com.example.spjappapk.util.DateTimeUtil.formatDisplay(order.timestamp)
    
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
            Row {
                when (order.status) {
                    "PROCESSED" -> Icon(Icons.Default.CloudDone, contentDescription = "Processed", tint = MaterialTheme.colorScheme.primary)
                    "APPROVED" -> Icon(Icons.Default.CheckCircle, contentDescription = "Approved", tint = Color(0xFF388E3C))
                    "REJECTED" -> Icon(Icons.Default.Cancel, contentDescription = "Rejected", tint = MaterialTheme.colorScheme.error)
                    else -> Icon(Icons.Default.CloudQueue, contentDescription = "Pending", tint = Color.Gray)
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
                        android.widget.Toast.makeText(context, "Nota disimpan ke galeri", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Gagal menyimpan nota", android.widget.Toast.LENGTH_SHORT).show()
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
                Text("Tgl: ${com.example.spjappapk.util.DateTimeUtil.formatDateOnly(sale.orderDate)}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
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
                        Text("${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} x Rp ${String.format(Locale.getDefault(), "%,.0f", item.displayPrice)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Text("Rp ${String.format(Locale.getDefault(), "%,.0f", item.displayPrice * item.quantity)}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                }
            }
            
            HorizontalDivider(color = Color.Black)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Rp${String.format(Locale.getDefault(), "%,.0f", sale.totalAmount)}", fontWeight = FontWeight.Bold, color = Color.Black)
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
