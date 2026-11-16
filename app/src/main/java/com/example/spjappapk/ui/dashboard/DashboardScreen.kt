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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
    onNavigateToTeamDetail: () -> Unit = {},
    onNavigateToBillingDetail: () -> Unit = {},
    onNavigateToPendingOmzet: () -> Unit = {},
    onNavigateToTodayVisits: () -> Unit = {},
    onNavigateToPendingSync: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isManagerOrOwner = uiState.userRole.lowercase() == "manager" || uiState.userRole.lowercase() == "owner"
    var selectedRouteForDetail by remember { mutableStateOf<DeliveryRouteSummary?>(null) }

    selectedRouteForDetail?.let { routeSummary ->
        AlertDialog(
            onDismissRequest = { selectedRouteForDetail = null },
            title = {
                Column {
                    Text(
                        text = "Jalur: ${routeSummary.routeName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${routeSummary.orderCount} Nota (SO & ON PROSES) • ${formatCurrency(routeSummary.totalNominal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6D28D9)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (routeSummary.invoices.isEmpty()) {
                        Text("Tidak ada nota pending pada jalur ini.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        routeSummary.invoices.forEach { inv ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = inv.customerName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Badge(
                                            containerColor = if (inv.status == "ON PROSES") Color(0xFFFEF3C7) else Color(0xFFDBEAFE),
                                            contentColor = if (inv.status == "ON PROSES") Color(0xFFB45309) else Color(0xFF1D4ED8)
                                        ) {
                                            Text(inv.status, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Nota: ${inv.nota} • ${inv.orderDateDisplay}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "Sales: ${inv.salesPersonName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatCurrency(inv.total),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRouteForDetail = null }) {
                    Text("Tutup")
                }
            }
        )
    }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.spjappapk.R.drawable.logo_spj),
                        contentDescription = "Logo SPJ",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Selamat Datang,",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.userName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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

                // Offline / Pending Queue Banner
                if (uiState.isOfflineMode || uiState.unsyncedCount > 0) {
                    Card(
                        onClick = onNavigateToPendingSync,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isOfflineMode) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                        ),
                        border = BorderStroke(1.dp, if (uiState.isOfflineMode) Color(0xFFF59E0B) else Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = if (uiState.isOfflineMode) Color(0xFFB45309) else Color(0xFF1D4ED8)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.isOfflineMode) "📴 Mode Offline Aktif" else "⏳ Ada ${uiState.unsyncedCount} Data Belum Sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isOfflineMode) Color(0xFF92400E) else Color(0xFF1E3A8A)
                                )
                                Text(
                                    text = if (uiState.isOfflineMode) {
                                        "Input Order SO, Kunjungan & Konsumen tetap tersimpan di HP (${uiState.unsyncedCount} antrean). Otomatis dikirim saat ada sinyal."
                                    } else {
                                        "Ketuk untuk melihat rincian Antrean Sync atau mengirim ke server sekarang."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isOfflineMode) Color(0xFFB45309) else Color(0xFF1D4ED8)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (uiState.isOfflineMode) Color(0xFFB45309) else Color(0xFF1D4ED8)
                            )
                        }
                    }
                }

                // Pintasan Cepat Kirim Laporan Kunjungan di Bagian Atas Dashboard
                Card(
                    onClick = onNavigateToVisits,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Kirim Laporan Kunjungan",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📸 Kirim Laporan Kunjungan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Pintasan pilih toko, cek GPS & foto langsung",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White
                        )
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
                        contentColor = Color(0xFFE65100),
                        onClick = onNavigateToBillingDetail
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onNavigateToBillingDetail,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFFE65100))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isManagerOrOwner) "Rincian Nota Tagihan Piutang (Tim)" else "Rincian Nota Tagihan Saya",
                        color = Color(0xFFE65100)
                    )
                }

                if (isManagerOrOwner) {
                    Text(
                        text = "* Klik kartu total omset di atas untuk rincian target per sales",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Antrean Omset (Pending/SO/Proses) - Sesuai SOP
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
                    onClick = onNavigateToPendingOmzet
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

                // Panel Nominal Nota Belum Dikirim per Jalur Pengiriman (Status: SO & ON PROSES) - Persis seperti Dashboard Desktop
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFF5F3FF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "🚚 JALUR PENGIRIMAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6D28D9),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Status: SO & ON PROSES",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nominal Nota Belum Dikirim per Jalur Pengiriman",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Ketuk baris jalur untuk melihat daftar nota",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.deliveryRouteBreakdown.isEmpty()) {
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✅ Semua Nota Telah Terkirim (0 Pending)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            uiState.deliveryRouteBreakdown.forEachIndexed { index, routeItem ->
                                if (index > 0) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedRouteForDetail = routeItem }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = routeItem.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${routeItem.orderCount} Nota Belum Kirim",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatCurrency(routeItem.totalNominal),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6D28D9)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Lihat Detail",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
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
                            contentColor = Color(0xFF388E3C),
                            onClick = onNavigateToTodayVisits
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
    return com.example.spjappapk.util.CurrencyUtil.formatRupiah(amount)
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
    contentColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        onClick = { onClick?.invoke() },
        enabled = onClick != null
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

