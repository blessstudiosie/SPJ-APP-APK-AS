package com.example.spjappapk.ui.salary

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.util.CurrencyUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryConfirmationScreen(
    viewModel: SalaryConfirmationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Tanda terima gaji berhasil dikonfirmasi!", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfirmasi Tanda Terima Gaji") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPendingConfirmations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.selectedConfirmation == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.pendingConfirmations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF388E3C),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tidak ada pengajuan gaji yang belum ditandatangani.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Semua slip gaji Anda telah dikonfirmasi atau belum dikirim oleh admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = { viewModel.loadPendingConfirmations() }) {
                        Text("Cek Ulang Pembaruan")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Selector if multiple pending confirmations exist
                if (uiState.pendingConfirmations.size > 1) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = "Periode: ${uiState.selectedConfirmation?.periodMonth ?: ""}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Periode Pengajuan Gaji") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            uiState.pendingConfirmations.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("Periode: ${item.periodMonth} (${CurrencyUtil.formatRupiah(item.takeHomePay)})") },
                                    onClick = {
                                        viewModel.selectConfirmation(item)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                uiState.error?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                uiState.selectedConfirmation?.let { conf ->
                    // Summary Slip Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rincian Gaji Periode ${conf.periodMonth}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Badge(containerColor = Color(0xFFE65100)) {
                                    Text("PENDING", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Total Take Home Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(
                                text = CurrencyUtil.formatRupiah(conf.takeHomePay),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail Breakdown Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Rincian Komponen Gaji", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            DetailRow("Gaji Pokok", CurrencyUtil.formatRupiah(conf.baseSalary))
                            DetailRow("Total Omset Sah", CurrencyUtil.formatRupiah(conf.totalOmset))
                            DetailRow("Total Komisi Omset", CurrencyUtil.formatRupiah(conf.totalKomisiOmset), Color(0xFF388E3C))
                            DetailRow("Total Kunjungan", "${conf.totalKunjungan} Titik")
                            DetailRow("Total Bonus Kunjungan", CurrencyUtil.formatRupiah(conf.totalBonusKunjungan), Color(0xFF388E3C))
                            DetailRow("Total Hari Transport", "${conf.totalHariTransport} Hari Kerja")

                            if (!conf.notes.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Catatan Admin:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(conf.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Digital Signature Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tanda Tangan Digital *",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Silakan gambar tanda tangan Anda di area putih di bawah ini sebagai bukti penerimaan sah.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SignatureCanvas(
                                onSignatureChanged = { isEmpty, provider ->
                                    viewModel.updateSignatureState(isEmpty, provider)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Button
                    Button(
                        onClick = { viewModel.confirmAndSignReceipt() },
                        enabled = !uiState.isSignatureEmpty && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengirim Konfirmasi...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Konfirmasi & Tanda Tangani")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
