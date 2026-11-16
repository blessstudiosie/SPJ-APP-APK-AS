package com.example.spjappapk.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SPJApplication
    
    val currentUser by app.sessionManager.userFlow.collectAsState(initial = null)
    
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            app.inventoryRepository,
            app.customerRepository,
            app.salesRepository,
            app.visitRepository,
            app.backupRepository,
            currentUser
        )
    }

    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val appUpdate by viewModel.appUpdate.collectAsState()
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    if (showChangePasswordDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSyncing) showChangePasswordDialog = false },
            title = { Text("Ubah Password Saya") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Akun: ${currentUser?.name ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it; dialogError = null },
                        label = { Text("Password Lama") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; dialogError = null },
                        label = { Text("Password Baru (Min. 4 Karakter)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; dialogError = null },
                        label = { Text("Konfirmasi Password Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanNew = newPass.trim()
                        val cleanConfirm = confirmPass.trim()
                        when {
                            oldPass.isBlank() -> dialogError = "Password lama wajib diisi."
                            cleanNew.length < 4 -> dialogError = "Password baru minimal terdiri dari 4 karakter."
                            cleanNew.equals("123", ignoreCase = true) || cleanNew.equals("ganti123", ignoreCase = true) ->
                                dialogError = "Password baru tidak boleh menggunakan password bawaan ('123')."
                            cleanNew != cleanConfirm -> dialogError = "Konfirmasi password baru tidak cocok."
                            else -> {
                                viewModel.changeMyPassword(oldPass, cleanNew) { success, message ->
                                    if (success) {
                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                        showChangePasswordDialog = false
                                    } else {
                                        dialogError = message
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSyncing
                ) {
                    Text(if (isSyncing) "Menyimpan..." else "Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePasswordDialog = false },
                    enabled = !isSyncing
                ) {
                    Text("Batal")
                }
            }
        )
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    if (appUpdate != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearUpdateState() },
            title = { Text("Pembaruan Tersedia") },
            text = {
                Column {
                    Text("Versi ${appUpdate!!.versionName} tersedia untuk diunduh.")
                    if (!appUpdate!!.releaseNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(appUpdate!!.releaseNotes!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appUpdate!!.downloadUrl))
                    context.startActivity(intent)
                    viewModel.clearUpdateState()
                }) {
                    Text("Download & Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearUpdateState() }) {
                    Text("Nanti")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
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
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Card 0: Keamanan Akun (Ubah Password)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Keamanan Akun", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Perbarui kata sandi (password) akun Anda secara berkala untuk menjaga keamanan akses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showChangePasswordDialog = true },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ubah Password Saya")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Card 1: Backup & Restore
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Cadangkan & Pulihkan Data", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Buat file cadangan lokal (backup) sebelum uninstall/reset aplikasi untuk mengamankan antrean data offline Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val defaultFileName = "SPJ_Backup_${DateTimeUtil.formatFileName()}.json"
                            createDocumentLauncher.launch(defaultFileName)
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buat File Backup (Ekspor)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pulihkan Data (Impor File Backup)")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Card 2: Manajemen Data
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Manajemen Data Server", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Gunakan opsi ini jika data stok atau pelanggan tidak akurat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.fullResetAndSync() },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reset & Sinkron Ulang Data")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.fixOrphanVisits() },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Build, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Perbaiki Antrean Kunjungan")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Card 3: Versi Aplikasi & Status
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Versi Aplikasi", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Versi Terpasang: ${com.example.spjappapk.BuildConfig.VERSION_NAME} (${com.example.spjappapk.BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    if (syncStatus.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = syncStatus,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.checkForUpdate() },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cek Pembaruan Aplikasi")
                    }
                }
            }
        }
    }
}
