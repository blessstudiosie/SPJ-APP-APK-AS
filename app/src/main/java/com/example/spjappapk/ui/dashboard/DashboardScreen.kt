package com.example.spjappapk.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSyncLog: () -> Unit = {},
    onNavigateToVisits: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToTeamDetail: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isManagerOrOwner = uiState.userRole.lowercase() == "manager" || uiState.userRole.lowercase() == "owner"

    // Update Dialog
    if (uiState.appUpdate != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Pembaruan Tersedia") },
            text = {
                Column {
                    Text("Versi terbaru (${uiState.appUpdate!!.versionName}) sudah tersedia.")
                    if (!uiState.appUpdate!!.releaseNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Apa yang baru:", fontWeight = FontWeight.Bold)
                        Text(uiState.appUpdate!!.releaseNotes!!)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.appUpdate!!.downloadUrl))
                    context.startActivity(intent)
                }) {
                    Text("Update Sekarang")
                }
            },
            dismissButton = {
                if (!uiState.appUpdate!!.isMandatory) {
                    TextButton(onClick = { /* Dismiss logic could be here */ }) {
                        Text("Nanti Saja")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SPJ Sales Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(badge = {
                            if (uiState.totalOrdersToday > 0) {
                                Badge { Text("${uiState.totalOrdersToday}") }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = { viewModel.manualSync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Manual Sync")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Selamat Datang, ${uiState.userName}!",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Sync Status Banner
                AnimatedVisibility(
                    visible = uiState.isSyncing || (uiState.syncStatus != "Idle" && uiState.syncStatus != "Sinkronisasi selesai"),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.syncStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onNavigateToSyncLog) {
                                Text("Detail", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                SummaryCard(
                    title = if (isManagerOrOwner) "Total Omzet Tim Bulan Ini" else "Omzet Anda Bulan Ini",
                    value = formatCurrency(uiState.monthlyOmzet),
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32),
                    onClick = if (isManagerOrOwner) onNavigateToTeamDetail else null
                )

                // Rincian Omset Sah (DONE & TEMPO)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "LUNAS (DONE)",
                        value = formatCurrency(uiState.monthlyMetrics.omsetDone),
                        count = uiState.monthlyMetrics.countDone,
                        color = Color(0xFFF1F8E9),
                        contentColor = Color(0xFF388E3C)
                    )
                    MetricMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "PIUTANG (TEMPO)",
                        value = formatCurrency(uiState.monthlyMetrics.omsetTempo),
                        count = uiState.monthlyMetrics.countTempo,
                        color = Color(0xFFFFF3E0),
                        contentColor = Color(0xFFE65100)
                    )
                }

                if (isManagerOrOwner) {
                    Text(
                        text = "* Klik kartu omset untuk rincian per sales",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Antrean Omset (Pending/SO/Proses) - Sesuai SOP
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HourglassEmpty, null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Antrean Omset (Proses)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0288D1))
                            Text(formatCurrency(uiState.monthlyMetrics.pendingNominal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                        }
                        Badge(containerColor = Color(0xFF0288D1)) {
                            Text("${uiState.monthlyMetrics.pendingCount} Nota", color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estimasi Input Hari Ini (Local Queue)
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        SummaryCard(
                            title = "Total Pesanan",
                            value = "${uiState.totalOrdersToday}",
                            label = "Order",
                            icon = Icons.Default.AddShoppingCart,
                            color = Color(0xFFE3F2FD),
                            contentColor = Color(0xFF1976D2)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        SummaryCard(
                            title = "Kunjungan Toko",
                            value = "${uiState.totalVisitsToday}",
                            label = "Toko",
                            icon = Icons.Default.Store,
                            color = Color(0xFFF1F8E9),
                            contentColor = Color(0xFF388E3C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Akses Cepat",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Card(
                    onClick = onNavigateToVisits,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Kirim Laporan Kunjungan", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Check-in lokasi & foto sekarang", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aplikasi SPJ v${com.example.spjappapk.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun formatCurrency(amount: Double): String {
    return try {
        "Rp ${String.format(java.util.Locale.getDefault(), "%,.0f", amount)}"
    } catch (e: Exception) {
        "Rp 0"
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    label: String = "",
    icon: ImageVector,
    color: Color,
    contentColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = contentColor.copy(alpha = 0.8f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, style = MaterialTheme.typography.headlineMedium, color = contentColor)
                if (label.isNotEmpty()) {
                    Text(text = " $label", style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun MetricMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    count: Int,
    color: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = contentColor, fontWeight = FontWeight.Bold)
            Text(text = "$count Nota", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f))
        }
    }
}

