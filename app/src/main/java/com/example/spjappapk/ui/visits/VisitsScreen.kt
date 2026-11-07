package com.example.spjappapk.ui.visits

import android.Manifest
import android.location.Location
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
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Locale

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
    var isCheckingEligibility by remember { mutableStateOf(false) }
    
    var alreadyVisitedTodayVisit by remember { mutableStateOf<Visit?>(null) }
    var outOfRadiusData by remember { mutableStateOf<Pair<Customer, Float>?>(null) }
    var autoLockCoordsForCustomer by remember { mutableStateOf<Customer?>(null) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null && selectedCustomer != null) {
            scope.launch {
                isCapturingLocation = true
                try {
                    val location = currentLocation ?: locationHelper.getCurrentLocation()
                    val lat = location?.latitude ?: 0.0
                    val lng = location?.longitude ?: 0.0

                    viewModel.addVisit(
                        customerId = selectedCustomer!!.remoteId,
                        customerName = selectedCustomer!!.name,
                        salesPersonName = currentSalesPersonName,
                        isNewCustomer = false,
                        lat = lat,
                        lng = lng,
                        photoPath = tempPhotoFile?.absolutePath,
                        notes = null
                    )

                    // Lock / Update customer coordinates if empty
                    if (autoLockCoordsForCustomer?.remoteId == selectedCustomer!!.remoteId && lat != 0.0 && lng != 0.0) {
                        viewModel.updateCustomerCoordinates(selectedCustomer!!.remoteId, lat, lng)
                        android.widget.Toast.makeText(context, "Koordinat lokasi outlet '${selectedCustomer!!.name}' berhasil dikunci!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isCapturingLocation = false
                    autoLockCoordsForCustomer = null
                    currentLocation = null
                }
            }
        }
    }

    val launchCameraForVisit = {
        val file = FileUtil.createImageFile(context)
        tempPhotoFile = file
        val uri = FileUtil.getUriForFile(context, file)
        cameraLauncher.launch(uri)
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
                    scope.launch {
                        isCapturingLocation = true // Reuse state for loading indication
                        try {
                            currentLocation = locationHelper.getCurrentLocation()
                            showCustomerPicker = true
                        } finally {
                            isCapturingLocation = false
                        }
                    }
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
                    showCustomerPicker = false
                    selectedCustomer = customer

                    scope.launch {
                        isCheckingEligibility = true
                        try {
                            // 1. Check if already visited today
                            val todayVisit = viewModel.getTodayVisitForCustomer(customer.remoteId)
                            if (todayVisit != null) {
                                alreadyVisitedTodayVisit = todayVisit
                                return@launch
                            }

                            // 2. Get current GPS location
                            val location = locationHelper.getCurrentLocation()
                            if (location == null) {
                                android.widget.Toast.makeText(context, "Gagal mendapatkan lokasi GPS. Pastikan GPS HP Anda aktif.", android.widget.Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            currentLocation = location

                            // 3. Check customer coordinates (Auto-Lock vs Distance Check)
                            val custLat = customer.latitude
                            val custLng = customer.longitude
                            val hasValidCoords = custLat != null && custLat != 0.0 && custLng != null && custLng != 0.0

                            if (!hasValidCoords) {
                                // Will auto-lock when saving
                                autoLockCoordsForCustomer = customer
                                android.widget.Toast.makeText(context, "Outlet belum memiliki koordinat. Lokasi GPS saat ini akan otomatis dikunci.", android.widget.Toast.LENGTH_SHORT).show()
                                launchCameraForVisit()
                            } else {
                                // Calculate distance
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(
                                    location.latitude,
                                    location.longitude,
                                    custLat!!,
                                    custLng!!,
                                    results
                                )
                                val distanceMeters = results[0]

                                if (distanceMeters > 100.0f) {
                                    // Out of radius warning!
                                    outOfRadiusData = Pair(customer, distanceMeters)
                                } else {
                                    // Within 100m radius!
                                    launchCameraForVisit()
                                }
                            }
                        } finally {
                            isCheckingEligibility = false
                        }
                    }
                },
                currentLocation = currentLocation,
                onCreateNew = { name, address, owner, phone ->
                    viewModel.createCustomer(name, address, owner, phone)
                    showCustomerPicker = false
                }
            )
        }

        if (isCheckingEligibility) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Memeriksa Validasi Kunjungan") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Mengecek riwayat kunjungan & koordinat GPS...")
                    }
                },
                confirmButton = { }
            )
        }

        if (alreadyVisitedTodayVisit != null) {
            AlertDialog(
                onDismissRequest = { alreadyVisitedTodayVisit = null },
                title = { Text("Outlet Sudah Dikunjungi Hari Ini") },
                text = {
                    val timeStr = com.example.spjappapk.util.DateTimeUtil.getFormat("HH:mm").format(java.util.Date(alreadyVisitedTodayVisit!!.timestamp))
                    Text("Outlet '${alreadyVisitedTodayVisit!!.customerName}' sudah dikunjungi hari ini pada pukul $timeStr.\n\nSesuai SOP, setiap outlet hanya dapat dikunjungi 1 kali per hari.")
                },
                confirmButton = {
                    Button(onClick = { alreadyVisitedTodayVisit = null }) {
                        Text("Tutup")
                    }
                }
            )
        }

        if (outOfRadiusData != null) {
            val (customer, distance) = outOfRadiusData!!
            AlertDialog(
                onDismissRequest = { outOfRadiusData = null },
                title = { Text("Peringatan Radius Kunjungan") },
                text = {
                    Text("Posisi Anda saat ini berjarak ${distance.toInt()} meter dari koordinat terdaftar outlet '${customer.name}' (Batas Maksimal: 100 meter).\n\nApakah Anda yakin ingin melanjutkan kunjungan?")
                },
                confirmButton = {
                    Button(onClick = {
                        outOfRadiusData = null
                        launchCameraForVisit()
                    }) {
                        Text("Lanjutkan Kunjungan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { outOfRadiusData = null }) {
                        Text("Batal")
                    }
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
    currentLocation: Location?,
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

    // Pair of Customer and Distance (in meters). Null distance if coords invalid or currentLocation is null.
    val sortedCustomers = remember(searchQuery, customers, currentLocation) {
        val filtered = if (searchQuery.isBlank()) {
            customers
        } else {
            customers.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                (it.address?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }

        filtered.map { customer ->
            val lat = customer.latitude
            val lng = customer.longitude
            var dist: Float? = null

            if (currentLocation != null && lat != null && lat != 0.0 && lng != null && lng != 0.0) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    currentLocation.latitude, currentLocation.longitude,
                    lat, lng,
                    results
                )
                dist = results[0]
            }
            Pair(customer, dist)
        }.sortedWith(
            compareBy<Pair<Customer, Float?>> { it.second == null } // Put nulls at the bottom
                .thenBy { it.second } // Then sort by distance ascending
                .thenBy { it.first.name } // Fallback to name
        )
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
                    } else if (sortedCustomers.isEmpty()) {
                        Text("Pelanggan '$searchQuery' tidak ditemukan.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(sortedCustomers) { (customer, distance) ->
                                ListItem(
                                    headlineContent = { Text(customer.name, fontWeight = FontWeight.Bold) },
                                    supportingContent = { 
                                        Column {
                                            Text(customer.address ?: "")
                                            if (distance != null) {
                                                val distText = if (distance >= 1000) {
                                                    String.format(Locale.getDefault(), "%.1f km", distance / 1000)
                                                } else {
                                                    "${distance.toInt()} meter"
                                                }
                                                Text(
                                                    text = "📍 Jarak: $distText",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (distance <= 100) Color(0xFF388E3C) else Color(0xFFE65100)
                                                )
                                            } else if (currentLocation != null) {
                                                Text(
                                                    text = "📍 Koordinat toko belum diatur",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.clickable { onCustomerSelected(customer) }
                                )
                                HorizontalDivider()
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
