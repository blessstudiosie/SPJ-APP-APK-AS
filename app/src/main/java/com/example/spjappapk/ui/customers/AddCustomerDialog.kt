package com.example.spjappapk.ui.customers

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.spjappapk.util.LocationHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        address: String,
        ownerName: String?,
        phone: String?,
        deliveryRoute: String?,
        creditLimit: Double,
        latitude: Double?,
        longitude: Double?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var creditLimitText by remember { mutableStateOf("") }
    
    val routes = listOf("LOBAR-MATARAM", "LOTIM", "LOTENG", "KLU", "SUMBAWA", "DOMPU", "BIMA")
    var selectedRoute by remember { mutableStateOf("LOBAR-MATARAM") }
    var expandedRoute by remember { mutableStateOf(false) }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val isFormValid = name.isNotBlank() && address.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Konsumen Baru") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Toko/Pelanggan *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Lengkap *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Nama Pemilik (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon/WA (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedRoute,
                    onExpandedChange = { expandedRoute = it }
                ) {
                    OutlinedTextField(
                        value = selectedRoute,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jalur Pengiriman") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoute) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRoute,
                        onDismissRequest = { expandedRoute = false }
                    ) {
                        routes.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    selectedRoute = r
                                    expandedRoute = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) creditLimitText = it },
                    label = { Text("Limit Piutang (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // GPS Location Lock Box
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (latitude != null) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Koordinat GPS Outlet", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (latitude != null && longitude != null) 
                                        "Lat: $latitude, Lng: $longitude" 
                                    else 
                                        "Belum dikunci",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (latitude != null) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                if (permissionsState.allPermissionsGranted) {
                                    scope.launch {
                                        isFetchingLocation = true
                                        try {
                                            val loc = locationHelper.getCurrentLocation()
                                            if (loc != null) {
                                                latitude = loc.latitude
                                                longitude = loc.longitude
                                                android.widget.Toast.makeText(context, "Lokasi GPS terkunci!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Gagal mendapatkan lokasi GPS. Pastikan GPS HP aktif.", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        } finally {
                                            isFetchingLocation = false
                                        }
                                    }
                                } else {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                            },
                            enabled = !isFetchingLocation,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mencari GPS...")
                            } else {
                                Icon(Icons.Default.MyLocation, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (latitude != null) "Perbarui Lokasi GPS Saat Ini" else "Kunci Lokasi GPS Saat Ini")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name.trim(),
                        address.trim(),
                        ownerName.trim().ifBlank { null },
                        phone.trim().ifBlank { null },
                        selectedRoute,
                        creditLimitText.toDoubleOrNull() ?: 0.0,
                        latitude,
                        longitude
                    )
                },
                enabled = isFormValid
            ) {
                Text("Simpan Konsumen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
