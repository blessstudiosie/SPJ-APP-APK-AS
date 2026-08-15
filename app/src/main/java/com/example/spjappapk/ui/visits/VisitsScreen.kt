package com.example.spjappapk.ui.visits

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.Visit
import com.example.spjappapk.util.FileUtil
import com.example.spjappapk.util.LocationHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VisitsScreen(viewModel: VisitsViewModel, currentSalesPersonName: String) {
    val visits by viewModel.visits.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null && selectedCustomer != null) {
            scope.launch {
                isCapturingLocation = true
                try {
                    val location = locationHelper.getCurrentLocation()
                    viewModel.addVisit(
                        customerId = selectedCustomer!!.remoteId,
                        customerName = selectedCustomer!!.name,
                        salesPersonName = currentSalesPersonName,
                        isNewCustomer = false,
                        lat = location?.latitude ?: 0.0,
                        lng = location?.longitude ?: 0.0,
                        photoPath = tempPhotoFile?.absolutePath,
                        notes = null
                    )
                } finally {
                    isCapturingLocation = false
                }
            }
        }
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
    )

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Field Visits") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            ) 
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (permissionsState.allPermissionsGranted) {
                    showCustomerPicker = true
                } else {
                    permissionsState.launchMultiplePermissionRequest()
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Log Visit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(visits) { visit ->
                VisitItem(
                    visit = visit,
                    onDelete = { viewModel.deleteVisit(visit.id) },
                    onEditNotes = { newNotes -> viewModel.updateVisitNotes(visit.id, newNotes) }
                )
                HorizontalDivider()
            }
        }

        if (showCustomerPicker) {
            CustomerPickerDialog(
                customers = customers,
                onDismiss = { showCustomerPicker = false },
                onCustomerSelected = { customer ->
                    selectedCustomer = customer
                    showCustomerPicker = false
                    
                    val file = FileUtil.createImageFile(context)
                    tempPhotoFile = file
                    val uri = FileUtil.getUriForFile(context, file)
                    cameraLauncher.launch(uri)
                },
                onCreateNew = { name, address, owner, phone ->
                    viewModel.createCustomer(name, address, owner, phone)
                    showCustomerPicker = false
                }
            )
        }

        if (isCapturingLocation) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Sinkronisasi Lokasi GPS") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sedang mengambil titik koordinat yang akurat...")
                        Text("Mohon tunggu sebentar.", style = MaterialTheme.typography.labelSmall)
                    }
                },
                confirmButton = { }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerDialog(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onCustomerSelected: (Customer) -> Unit,
    onCreateNew: (String, String?, String?, String?) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newOwner by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCustomers = remember(searchQuery, customers) {
        if (searchQuery.isBlank()) {
            customers
        } else {
            customers.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                (it.address?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showCreateForm) "Daftarkan Pelanggan Baru" else "Pilih Pelanggan") },
        text = {
            if (showCreateForm) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama Toko/Pelanggan *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Alamat Lengkap *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newOwner,
                        onValueChange = { newOwner = it },
                        label = { Text("Nama Pemilik") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Nomor Telepon/WA") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Text("* Wajib diisi", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Cari Pelanggan...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showCreateForm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pelanggan Tidak Ditemukan?")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (customers.isEmpty()) {
                        Text("Belum ada data pelanggan. Silakan buat baru.")
                    } else if (filteredCustomers.isEmpty()) {
                        Text("Pelanggan '$searchQuery' tidak ditemukan.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(filteredCustomers) { customer ->
                                ListItem(
                                    headlineContent = { Text(customer.name) },
                                    supportingContent = { Text(customer.address ?: "") },
                                    modifier = Modifier.clickable { onCustomerSelected(customer) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateForm) {
                Button(
                    onClick = {
                        onCreateNew(newName, newAddress, newOwner, newPhone)
                        showCreateForm = false
                    },
                    enabled = newName.isNotEmpty() && newAddress.isNotEmpty()
                ) {
                    Text("Simpan & Pilih")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun VisitItem(
    visit: Visit,
    onDelete: () -> Unit,
    onEditNotes: (String) -> Unit
) {
    val dateText = com.example.spjappapk.util.DateTimeUtil.formatDisplay(visit.timestamp)
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editNotes by remember { mutableStateOf(visit.notes ?: "") }

    ListItem(
        headlineContent = { 
            Text(
                text = visit.customerName.ifEmpty { "Kunjungan Tanpa Nama" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        supportingContent = {
            Column {
                Text("Waktu: $dateText", style = MaterialTheme.typography.bodySmall)
                if (visit.customerName.isEmpty()) {
                    Text("ID: ${visit.customerId}", style = MaterialTheme.typography.bodySmall)
                }
                if (!visit.notes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Catatan: ${visit.notes}", style = MaterialTheme.typography.bodyMedium)
                }
                if (visit.photoPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = visit.photoPath,
                        contentDescription = "Visit Photo",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(4.dp)
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (visit.syncStatus == "SYNCED" || visit.syncStatus == "SUCCESS") {
                    Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.CloudQueue, contentDescription = "Pending", tint = MaterialTheme.colorScheme.error)
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Catatan") },
                            onClick = {
                                showEditDialog = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus") },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    )

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Catatan Kunjungan") },
            text = {
                OutlinedTextField(
                    value = editNotes,
                    onValueChange = { editNotes = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onEditNotes(editNotes)
                    showEditDialog = false
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
