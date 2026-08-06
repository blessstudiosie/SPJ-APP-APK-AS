package com.example.spjappapk.ui.visits

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VisitsScreen(viewModel: VisitsViewModel) {
    val visits by viewModel.visits.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null && selectedCustomer != null) {
            scope.launch {
                val location = locationHelper.getCurrentLocation()
                viewModel.addVisit(
                    customerId = selectedCustomer!!.remoteId,
                    lat = location?.latitude ?: 0.0,
                    lng = location?.longitude ?: 0.0,
                    photoPath = tempPhotoFile?.absolutePath
                )
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
        topBar = { TopAppBar(title = { Text("Field Visits") }) },
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
                VisitItem(visit = visit)
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
                onCreateNew = { name, address ->
                    viewModel.createCustomer(name, address)
                    // After creating, we still need to select it or wait for sync.
                    // For simplicity, let's just close and wait for sync or 
                    // manually select if repo update was fast enough.
                    showCustomerPicker = false
                }
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
    onCreateNew: (String, String?) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showCreateForm) "Daftarkan Pelanggan Baru" else "Pilih Pelanggan") },
        text = {
            if (showCreateForm) {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama Toko/Pelanggan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Alamat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column {
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
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(customers) { customer ->
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
                        onCreateNew(newName, newAddress)
                        showCreateForm = false
                    },
                    enabled = newName.isNotEmpty()
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
fun VisitItem(visit: Visit) {
    val date = Date(visit.timestamp)
    val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    ListItem(
        headlineContent = { Text("Visit at ${format.format(date)}") },
        supportingContent = {
            Column {
                Text("Customer ID: ${visit.customerId}")
                Text("Lat: ${visit.locationLat}, Lng: ${visit.locationLng}")
                if (visit.photoPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = visit.photoPath,
                        contentDescription = "Visit Photo",
                        modifier = Modifier.size(100.dp)
                    )
                }
            }
        },
        trailingContent = {
            if (visit.syncStatus == "SYNCED") {
                Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Default.CloudQueue, contentDescription = "Pending", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}
