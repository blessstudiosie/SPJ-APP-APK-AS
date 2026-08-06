package com.example.spjappapk.ui.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.local.entities.SalesOrderItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesLedgerScreen(viewModel: SalesViewModel) {
    val sales by viewModel.sales.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sales Ledger") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Order")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(sales) { order ->
                SaleItem(order = order)
                HorizontalDivider()
            }
        }

        if (showDialog) {
            AddSaleDialog(
                products = products,
                customers = customers,
                onDismiss = { showDialog = false },
                onConfirm = { customerId, notes, items ->
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    viewModel.addOrder(customerId, date, notes, items)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun SaleItem(order: SalesOrder) {
    val date = Date(order.timestamp)
    val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    
    ListItem(
        headlineContent = { Text("Order #${order.id}") },
        supportingContent = { 
            Column {
                Text("Customer: ${order.customerId}")
                Text(format.format(date))
            }
        },
        trailingContent = { 
            Row {
                if (order.status == "PROCESSED") {
                    Icon(Icons.Default.CloudDone, contentDescription = "Processed", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.CloudQueue, contentDescription = "Pending", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleDialog(
    products: List<Inventory>,
    customers: List<Customer>,
    onDismiss: () -> Unit, 
    onConfirm: (String, String?, List<SalesOrderItem>) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    val orderItems = remember { mutableStateListOf<SalesOrderItem>() }
    var notes by remember { mutableStateOf("") }
    
    var customerExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }
    var selectedProductForAdd by remember { mutableStateOf<Inventory?>(null) }
    var quantityForAdd by remember { mutableStateOf("1") }

    val totalOrderPrice = orderItems.sumOf { it.price * it.quantity }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Sales Order (Multi-Item)") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Customer Selection
                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = !customerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = { Text(customer.name) },
                                onClick = {
                                    selectedCustomer = customer
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Add Items", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                
                // Add Item Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = productExpanded,
                            onExpandedChange = { productExpanded = !productExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProductForAdd?.name ?: "Product",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = productExpanded,
                                onDismissRequest = { productExpanded = false }
                            ) {
                                products.forEach { product ->
                                    DropdownMenuItem(
                                        text = { Text("${product.name} (Rp${product.price})") },
                                        onClick = {
                                            selectedProductForAdd = product
                                            productExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = quantityForAdd,
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantityForAdd = it },
                        modifier = Modifier.width(70.dp),
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    IconButton(onClick = {
                        selectedProductForAdd?.let { prod ->
                            val qty = quantityForAdd.toIntOrNull() ?: 0
                            if (qty > 0) {
                                orderItems.add(SalesOrderItem(prod.remoteId, qty, prod.price))
                                selectedProductForAdd = null
                                quantityForAdd = "1"
                            }
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // List of Added Items
                orderItems.forEachIndexed { index, item ->
                    val prodName = products.find { it.remoteId == item.product_id }?.name ?: "Unknown"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}. $prodName x${item.quantity}", modifier = Modifier.weight(1f))
                        Text("Rp${String.format(Locale.getDefault(), "%,.0f", item.price * item.quantity)}")
                        IconButton(onClick = { orderItems.removeAt(index) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (orderItems.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Order:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Rp ${String.format(Locale.getDefault(), "%,.0f", totalOrderPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCustomer != null && orderItems.isNotEmpty(),
                onClick = {
                    onConfirm(selectedCustomer!!.remoteId, notes, orderItems.toList())
                }
            ) {
                Text("Kirim SO")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
