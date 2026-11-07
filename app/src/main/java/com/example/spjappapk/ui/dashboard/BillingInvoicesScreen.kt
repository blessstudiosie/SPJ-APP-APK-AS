package com.example.spjappapk.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.util.BusinessRevenueHelper
import com.example.spjappapk.util.CurrencyUtil
import com.example.spjappapk.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingInvoicesScreen(
    viewModel: DashboardViewModel,
    currentUser: SalesPerson?,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (saleId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isManagerOrOwner = currentUser?.role?.trim()?.lowercase() in listOf("manager", "owner", "admin")

    val salesMasterFlow = viewModel.app.salesRepository.allSalesMaster.collectAsState(initial = emptyList())
    val staffListFlow = viewModel.app.database.salesPersonDao().getAllSalesPersons().collectAsState(initial = emptyList())
    val customerListFlow = viewModel.app.customerRepository.allCustomers.collectAsState(initial = emptyList())

    val allSalesMaster = salesMasterFlow.value
    val staffList = staffListFlow.value
    val customerList = customerListFlow.value

    var searchQuery by remember { mutableStateOf("") }

    // Filter TEMPO/Unpaid invoices (where status is TEMPO or remaining > 0)
    val tempoInvoices = remember(allSalesMaster, customerList, currentUser, isManagerOrOwner, searchQuery) {
        val activeTempo = allSalesMaster.filter { sale ->
            sale.isDeleted != true &&
            BusinessRevenueHelper.isValidOmsetStatus(sale.status) &&
            sale.status.trim().uppercase() == "TEMPO" &&
            sale.remaining > 0
        }.map { sale ->
            sale.copy(customerName = BusinessRevenueHelper.resolveCustomerName(sale.customerName, sale.customerId, customerList))
        }

        val roleFiltered = if (isManagerOrOwner) {
            activeTempo
        } else {
            activeTempo.filter { sale ->
                val resolvedName = BusinessRevenueHelper.resolveSalesPersonName(sale, staffList)
                sale.salesPersonId?.equals(currentUser?.id, ignoreCase = true) == true ||
                resolvedName.equals(currentUser?.name, ignoreCase = true)
            }
        }

        if (searchQuery.isBlank()) {
            roleFiltered
        } else {
            roleFiltered.filter { sale ->
                sale.nota.contains(searchQuery, ignoreCase = true) ||
                (sale.customerName?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (sale.salesName?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }
    }

    val totalRemaining = tempoInvoices.sumOf { it.remaining }

    // Grouping by Sales Person for Manager
    val groupedBySales = remember(tempoInvoices, staffList) {
        tempoInvoices.groupBy { sale ->
            BusinessRevenueHelper.resolveSalesPersonName(sale, staffList)
        }.toList().sortedByDescending { (_, sales) -> sales.sumOf { it.remaining } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Tagihan Piutang") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Header Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isManagerOrOwner) "Total Piutang Tim (Harus Ditagih)" else "Total Piutang Anda",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = CurrencyUtil.formatRupiah(totalRemaining),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                        Badge(containerColor = Color(0xFFE65100)) {
                            Text("${tempoInvoices.size} Nota", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Cari No Nota, Customer, atau Sales...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (tempoInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Tidak ada nota tagihan TEMPO saat ini." else "Nota tagihan tidak ditemukan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (isManagerOrOwner) {
                    // MANAGER VIEW: Grouped by Sales Person
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupedBySales) { (salesName, salesInvoices) ->
                            SalesGroupHeaderCard(
                                salesName = salesName,
                                invoices = salesInvoices,
                                onNavigateToPayment = onNavigateToPayment
                            )
                        }
                    }
                } else {
                    // SALES VIEW: Direct list of their invoices
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tempoInvoices) { sale ->
                            InvoiceBillingCard(
                                sale = sale,
                                onNavigateToPayment = onNavigateToPayment
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesGroupHeaderCard(
    salesName: String,
    invoices: List<SalesMaster>,
    onNavigateToPayment: (saleId: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalSalesRemaining = invoices.sumOf { it.remaining }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
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
                        text = "Total Piutang: ${CurrencyUtil.formatRupiah(totalSalesRemaining)} • ${invoices.size} Nota",
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
                    invoices.forEach { sale ->
                        InvoiceBillingCard(
                            sale = sale,
                            onNavigateToPayment = onNavigateToPayment
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceBillingCard(
    sale: SalesMaster,
    onNavigateToPayment: (saleId: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nota: ${sale.nota}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                val dateStr = DateTimeUtil.formatDateOnly(sale.deliveryDate ?: sale.orderDate)
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = sale.customerName ?: "Pelanggan Tanpa Nama",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total: ${CurrencyUtil.formatRupiah(sale.totalAmount)} (Lunas: ${CurrencyUtil.formatRupiah(sale.paid)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sisa Tagihan: ${CurrencyUtil.formatRupiah(sale.remaining)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }

                Button(
                    onClick = { onNavigateToPayment(sale.id) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bayar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
