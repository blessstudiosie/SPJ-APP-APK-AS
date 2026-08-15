package com.example.spjappapk.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.data.local.entities.SalesOrderItem
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSalesOrderScreen(
    viewModel: SalesViewModel,
    onNavigateBack: () -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()

    var searchQueryCustomer by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    val orderItems = remember { mutableStateListOf<SalesOrderItem>() }
    var notes by remember { mutableStateOf("") }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showCreateCustomerDialog by remember { mutableStateOf(false) }

    val totalAmount = orderItems.sumOf { it.price * it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input Pesanan Baru") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Estimasi:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Rp ${String.format(Locale.getDefault(), "%,.0f", totalAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            selectedCustomer?.let { customer ->
                                val date = com.example.spjappapk.util.DateTimeUtil.getCurrentDateString()
                                viewModel.addOrder(
                                    customer.remoteId,
                                    customer.name,
                                    viewModel.currentUser?.name ?: "Unknown",
                                    date,
                                    notes,
                                    orderItems.toList()
                                )
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedCustomer != null && orderItems.isNotEmpty()
                    ) {
                        Text("Kirim Pesanan ke Antrean")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Informasi Pelanggan", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Autocomplete Customer
            Box {
                OutlinedTextField(
                    value = if (selectedCustomer != null) selectedCustomer!!.name else searchQueryCustomer,
                    onValueChange = {
                        searchQueryCustomer = it
                        selectedCustomer = null
                        isCustomerDropdownExpanded = it.isNotEmpty()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cari/Ketik Nama Toko") },
                    leadingIcon = { Icon(Icons.Default.Store, null) },
                    trailingIcon = {
                        if (selectedCustomer != null) {
                            IconButton(onClick = {
                                selectedCustomer = null
                                searchQueryCustomer = ""
                            }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    singleLine = true
                )

                if (isCustomerDropdownExpanded) {
                    val filteredCustomers = customers.filter {
                        it.name.contains(searchQueryCustomer, ignoreCase = true)
                    }.take(5)

                    if (filteredCustomers.isNotEmpty() || searchQueryCustomer.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column {
                                filteredCustomers.forEach { customer ->
                                    ListItem(
                                        headlineContent = { Text(customer.name) },
                                        supportingContent = { Text(customer.address ?: "") },
                                        modifier = Modifier.clickable {
                                            selectedCustomer = customer
                                            searchQueryCustomer = customer.name
                                            isCustomerDropdownExpanded = false
                                        }
                                    )
                                    HorizontalDivider()
                                }
                                
                                if (filteredCustomers.isEmpty()) {
                                    ListItem(
                                        headlineContent = { Text("Konsumen tidak ditemukan", color = MaterialTheme.colorScheme.error) },
                                        supportingContent = { Text("Klik untuk daftarkan baru") },
                                        modifier = Modifier.clickable {
                                            showCreateCustomerDialog = true
                                            isCustomerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar Barang", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAddItemDialog = true }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah Barang")
                }
            }
            
            HorizontalDivider()

            if (orderItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada barang ditambahkan", color = Color.Gray)
                }
            } else {
                orderItems.forEachIndexed { index, item ->
                    ListItem(
                        headlineContent = { Text(item.displayProductName) },
                        supportingContent = {
                            Text("${item.quantity} x Rp ${String.format(Locale.getDefault(), "%,.0f", item.displayPrice)}")
                        },
                        trailingContent = {
                            IconButton(onClick = { orderItems.removeAt(index) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Pesanan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(Modifier.height(100.dp)) // Extra space for bottom bar
        }
    }

    if (showAddItemDialog) {
        AddItemDialog(
            products = products,
            onDismiss = { showAddItemDialog = false },
            onConfirm = { newItem ->
                orderItems.add(newItem)
                showAddItemDialog = false
            }
        )
    }

    if (showCreateCustomerDialog) {
        // Reuse CustomerPickerDialog or a similar form
        // For simplicity here, let's just make a simple dialog
        var newName by remember { mutableStateOf(searchQueryCustomer) }
        var newAddress by remember { mutableStateOf("") }
        var newOwner by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateCustomerDialog = false },
            title = { Text("Daftarkan Pelanggan Baru") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nama Toko *") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newAddress, onValueChange = { newAddress = it }, label = { Text("Alamat Lengkap *") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newOwner, onValueChange = { newOwner = it }, label = { Text("Nama Pemilik") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Nomor Telepon/WA") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newId = java.util.UUID.randomUUID().toString()
                        val customer = Customer(
                            remoteId = newId,
                            name = newName,
                            address = newAddress,
                            ownerName = newOwner,
                            phone = newPhone,
                            salesPersonId = viewModel.currentUser?.id,
                            salesPersonName = viewModel.currentUser?.name
                        )
                        // Trigger creation in repository (through VM)
                        // Since SalesViewModel might not have createCustomer, let's assume we need to add it or use repository directly
                        // To keep it simple, let's navigate back to visits to register or add it to SalesViewModel
                        // Better: Add to SalesViewModel
                        viewModel.addCustomerManually(customer)
                        selectedCustomer = customer
                        searchQueryCustomer = customer.name
                        showCreateCustomerDialog = false
                    },
                    enabled = newName.isNotBlank() && newAddress.isNotBlank()
                ) {
                    Text("Simpan & Pilih")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateCustomerDialog = false }) { Text("Batal") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    products: List<Inventory>,
    onDismiss: () -> Unit,
    onConfirm: (SalesOrderItem) -> Unit
) {
    var searchQueryProduct by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Inventory?>(null) }
    var isProductDropdownExpanded by remember { mutableStateOf(false) }

    var quantity by remember { mutableStateOf("1") }
    var manualPrice by remember { mutableStateOf("") }
    var priceCategory by remember { mutableIntStateOf(0) } // 0: R, 1: SG, 2: G, 3: P, 4: Manual

    val currentPrice = when (priceCategory) {
        0 -> selectedProduct?.priceR ?: 0.0
        1 -> selectedProduct?.priceSG ?: 0.0
        2 -> selectedProduct?.priceG ?: 0.0
        3 -> selectedProduct?.priceP ?: 0.0
        else -> manualPrice.toDoubleOrNull() ?: 0.0
    }

    val isPriceValid = selectedProduct == null || currentPrice >= selectedProduct!!.priceP
    val qtyInt = quantity.toIntOrNull() ?: 0
    val isStockValid = selectedProduct == null || qtyInt <= selectedProduct!!.stockQuantity

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Pilih Produk", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                // Autocomplete Product
                Box {
                    OutlinedTextField(
                        value = if (selectedProduct != null) selectedProduct!!.name else searchQueryProduct,
                        onValueChange = {
                            searchQueryProduct = it
                            selectedProduct = null
                            isProductDropdownExpanded = it.isNotEmpty()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ketik Nama Produk") },
                        trailingIcon = {
                            if (selectedProduct != null) {
                                IconButton(onClick = {
                                    selectedProduct = null
                                    searchQueryProduct = ""
                                }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        }
                    )

                    if (isProductDropdownExpanded) {
                        val filteredProducts = products.filter {
                            it.name.contains(searchQueryProduct, ignoreCase = true)
                        }.take(5)

                        if (filteredProducts.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column {
                                    filteredProducts.forEach { product ->
                                        ListItem(
                                            headlineContent = { 
                                                Text(product.name, fontSize = 18.sp, fontWeight = FontWeight.Bold) 
                                            },
                                            supportingContent = { 
                                                Text("Stok: ${product.stockQuantity} ${product.unit} | Rp ${product.priceR}") 
                                            },
                                            modifier = Modifier.clickable {
                                                selectedProduct = product
                                                searchQueryProduct = product.name
                                                isProductDropdownExpanded = false
                                                manualPrice = product.priceR.toInt().toString()
                                            }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedProduct != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Informasi Stok", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Tersedia: ${selectedProduct!!.stockQuantity} ${selectedProduct!!.unit}",
                        color = if (isStockValid) Color.Unspecified else MaterialTheme.colorScheme.error,
                        fontWeight = if (!isStockValid) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                        label = { Text("Jumlah (Qty)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isStockValid,
                        supportingText = { if (!isStockValid) Text("Stok tidak cukup!") }
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Pilih Harga", style = MaterialTheme.typography.labelLarge)
                    
                    val priceOptions = listOf(
                        "Retail (R): Rp${selectedProduct!!.priceR}",
                        "SG: Rp${selectedProduct!!.priceSG}",
                        "Grosir (G): Rp${selectedProduct!!.priceG}",
                        "Partai (P): Rp${selectedProduct!!.priceP}",
                        "Manual"
                    )

                    Column {
                        priceOptions.forEachIndexed { index, option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { priceCategory = index }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = priceCategory == index, onClick = { priceCategory = index })
                                Text(option, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (priceCategory == 4) {
                        OutlinedTextField(
                            value = manualPrice,
                            onValueChange = { if (it.all { c -> c.isDigit() }) manualPrice = it },
                            label = { Text("Input Harga Manual") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = !isPriceValid,
                            supportingText = { 
                                if (!isPriceValid) Text("Harga minimal Rp ${selectedProduct!!.priceP} (Harga P)") 
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            selectedProduct?.let { prod ->
                                onConfirm(
                                    SalesOrderItem(
                                        productId = prod.remoteId,
                                        itemName = prod.name,
                                        quantity = qtyInt.toDouble(),
                                        price = currentPrice,
                                        priceCategory = when (priceCategory) {
                                            0 -> "R"
                                            1 -> "SG"
                                            2 -> "G"
                                            3 -> "P"
                                            else -> "R"
                                        }
                                    )
                                )
                            }
                        },
                        enabled = selectedProduct != null && qtyInt > 0 && isPriceValid && isStockValid
                    ) {
                        Text("Tambah")
                    }
                }
            }
        }
    }
}
