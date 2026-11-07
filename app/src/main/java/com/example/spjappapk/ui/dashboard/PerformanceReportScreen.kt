package com.example.spjappapk.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.models.CompanyPerformanceSummary
import com.example.spjappapk.data.local.models.PriceCategoryBreakdown
import com.example.spjappapk.data.local.models.SalesPersonPerformance
import com.example.spjappapk.data.local.models.UnrealizedOmsetBreakdown
import com.example.spjappapk.util.CurrencyUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceReportScreen(
    viewModel: PerformanceReportViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPendingOmzet: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Kinerja") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // If Manager/Owner, show 3 Tabs. If Sales, show Personal Title.
                if (uiState.isManagerOrOwner) {
                    TabRow(selectedTabIndex = uiState.selectedTab) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            text = { Text("Keseluruhan") },
                            icon = { Icon(Icons.Default.Business, null) }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            text = { Text("Per Sales") },
                            icon = { Icon(Icons.Default.Group, null) }
                        )
                        Tab(
                            selected = uiState.selectedTab == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            text = { Text("Pribadi") },
                            icon = { Icon(Icons.Default.Person, null) }
                        )
                    }
                }

                when (uiState.selectedTab) {
                    0 -> CompanySummaryView(
                        summary = uiState.summary,
                        isLoadingDetails = uiState.isLoadingDetails,
                        onNavigateToPendingOmzet = onNavigateToPendingOmzet
                    )
                    1 -> SalesBreakdownView(breakdown = uiState.summary.salesBreakdown)
                    2 -> PersonalPerformanceView(
                        perf = uiState.myPerformance,
                        isLoadingDetails = uiState.isLoadingDetails,
                        onNavigateToPendingOmzet = onNavigateToPendingOmzet
                    )
                }
            }
        }
    }
}

@Composable
fun CompanySummaryView(
    summary: CompanyPerformanceSummary,
    isLoadingDetails: Boolean = false,
    onNavigateToPendingOmzet: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Target vs Achievement Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pencapaian Omset Perusahaan (Bulan Ini)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Omset Terrealisasi", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = CurrencyUtil.formatRupiah(summary.totalCompanyOmset),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target Perusahaan", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = CurrencyUtil.formatRupiah(summary.totalCompanyTarget),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                val progress = (summary.companyAchievementPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pencapaian: ${String.format(java.util.Locale.getDefault(), "%.1f", summary.companyAchievementPercentage)}% dari Target",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stat Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Total Kunjungan",
                value = "${summary.totalCompanyVisits}",
                label = "Kunjungan",
                icon = Icons.Default.LocationOn,
                color = Color(0xFFE3F2FD),
                contentColor = Color(0xFF1565C0)
            )
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Pelanggan Baru",
                value = "${summary.totalNewCustomers}",
                label = "Toko",
                icon = Icons.Default.PersonAdd,
                color = Color(0xFFF1F8E9),
                contentColor = Color(0xFF2E7D32)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Breakdowns Lunas vs Piutang
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Omset LUNAS (DONE)",
                value = CurrencyUtil.formatRupiah(summary.metrics.omsetDone),
                label = "${summary.metrics.countDone} Nota",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32)
            )
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Omset TEMPO (Piutang)",
                value = CurrencyUtil.formatRupiah(summary.metrics.omsetTempo),
                label = "${summary.metrics.countTempo} Nota",
                icon = Icons.Default.Schedule,
                color = Color(0xFFFFF3E0),
                contentColor = Color(0xFFE65100)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        UnrealizedOmsetCard(
            title = "Omset Belum Terealisasi (Tim)",
            unrealizedBreakdown = summary.unrealizedBreakdown,
            realizedOmset = summary.totalCompanyOmset,
            targetOmset = summary.totalCompanyTarget,
            onClickDetail = onNavigateToPendingOmzet
        )

        Spacer(modifier = Modifier.height(16.dp))

        PriceCategoryBreakdownCard(
            title = "Rincian Total Omset per Kategori Harga",
            breakdown = summary.categoryBreakdown,
            isLoadingDetails = isLoadingDetails
        )
    }
}

@Composable
fun SalesBreakdownView(breakdown: List<SalesPersonPerformance>) {
    var selectedSalesPerf by remember { mutableStateOf<SalesPersonPerformance?>(null) }

    if (breakdown.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada data kinerja sales bulan ini.")
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "* Klik kartu sales untuk melihat rincian & penjelasan status.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(breakdown) { item ->
                    SalesPerformanceCard(
                        perf = item,
                        onClick = { selectedSalesPerf = item }
                    )
                }
            }
        }
    }

    if (selectedSalesPerf != null) {
        SalesPerformanceDetailDialog(
            perf = selectedSalesPerf!!,
            onDismiss = { selectedSalesPerf = null }
        )
    }
}

@Composable
fun PersonalPerformanceView(
    perf: SalesPersonPerformance,
    isLoadingDetails: Boolean = false,
    onNavigateToPendingOmzet: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pencapaian Pribadi (${perf.salesPersonName})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Omset Pribadi", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = CurrencyUtil.formatRupiah(perf.totalOmset),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target Pribadi", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = CurrencyUtil.formatRupiah(perf.targetOmset),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val progress = (perf.achievementPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pencapaian: ${String.format(java.util.Locale.getDefault(), "%.1f", perf.achievementPercentage)}% dari Target",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Kunjungan Saya",
                value = "${perf.totalVisits}",
                label = "${perf.uniqueOutletsVisited} Toko Unik",
                icon = Icons.Default.LocationOn,
                color = Color(0xFFE3F2FD),
                contentColor = Color(0xFF1565C0)
            )
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Toko Baru",
                value = "${perf.newCustomersCount}",
                label = "Pelanggan Baru",
                icon = Icons.Default.PersonAdd,
                color = Color(0xFFF1F8E9),
                contentColor = Color(0xFF2E7D32)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Penjualan LUNAS",
                value = CurrencyUtil.formatRupiah(perf.omsetDone),
                label = "Telah Dibayar",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32)
            )
            MetricStatCard(
                modifier = Modifier.weight(1f),
                title = "Penjualan TEMPO",
                value = CurrencyUtil.formatRupiah(perf.omsetTempo),
                label = "Sisa Piutang",
                icon = Icons.Default.Schedule,
                color = Color(0xFFFFF3E0),
                contentColor = Color(0xFFE65100)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        UnrealizedOmsetCard(
            title = "Omset Belum Terealisasi Saya",
            unrealizedBreakdown = perf.unrealizedBreakdown,
            realizedOmset = perf.totalOmset,
            targetOmset = perf.targetOmset,
            onClickDetail = onNavigateToPendingOmzet
        )

        Spacer(modifier = Modifier.height(16.dp))

        PriceCategoryBreakdownCard(
            title = "Rincian Total Omset per Kategori Harga",
            breakdown = perf.categoryBreakdown,
            isLoadingDetails = isLoadingDetails
        )
    }
}

@Composable
fun SalesPerformanceCard(
    perf: SalesPersonPerformance,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = perf.salesPersonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val statusText = when {
                    perf.achievementPercentage >= 100.0 -> "🎯 Target Tercapai"
                    perf.achievementPercentage >= 70.0 -> "📈 On Track"
                    else -> "⚠️ Perlu Perhatian"
                }
                
                val statusColor = when {
                    perf.achievementPercentage >= 100.0 -> Color(0xFF2E7D32)
                    perf.achievementPercentage >= 70.0 -> Color(0xFF1565C0)
                    else -> Color(0xFFE65100)
                }

                Badge(containerColor = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Omset Realisasi", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = CurrencyUtil.formatRupiah(perf.totalOmset),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Belum Terealisasi", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = CurrencyUtil.formatRupiah(perf.pendingNominal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target Sales", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = CurrencyUtil.formatRupiah(perf.targetOmset),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progress = (perf.achievementPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pencapaian: ${String.format(java.util.Locale.getDefault(), "%.1f", perf.achievementPercentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${perf.totalVisits} Kunjungan • ${perf.newCustomersCount} Toko Baru",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
            if (label.isNotEmpty()) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun SalesPerformanceDetailDialog(
    perf: SalesPersonPerformance,
    onDismiss: () -> Unit
) {
    val statusText = when {
        perf.achievementPercentage >= 100.0 -> "🎯 Target Tercapai"
        perf.achievementPercentage >= 70.0 -> "📈 On Track"
        else -> "⚠️ Perlu Perhatian"
    }

    val statusExplanation = when {
        perf.achievementPercentage >= 100.0 -> 
            "Pencapaian omset ${perf.salesPersonName} telah mencapai ${String.format(java.util.Locale.getDefault(), "%.1f", perf.achievementPercentage)}% dan telah memenuhi/melampaui target bulan ini."
        perf.achievementPercentage >= 70.0 -> 
            "Pencapaian omset ${perf.salesPersonName} berada di angka ${String.format(java.util.Locale.getDefault(), "%.1f", perf.achievementPercentage)}%. Performa sudah cukup baik dan mendekati target bulanan."
        else -> {
            val shortfall = (perf.targetOmset - perf.totalOmset).coerceAtLeast(0.0)
            "Pencapaian omset ${perf.salesPersonName} baru mencapai ${String.format(java.util.Locale.getDefault(), "%.1f", perf.achievementPercentage)}% (di bawah batas minimal 70%). Masih kurang ${CurrencyUtil.formatRupiah(shortfall)} dari target. Perlu peningkatan frekuensi kunjungan toko dan dorongan penjualan."
        }
    }

    val containerColor = when {
        perf.achievementPercentage >= 100.0 -> Color(0xFFE8F5E9)
        perf.achievementPercentage >= 70.0 -> Color(0xFFE3F2FD)
        else -> Color(0xFFFFF3E0)
    }

    val contentColor = when {
        perf.achievementPercentage >= 100.0 -> Color(0xFF2E7D32)
        perf.achievementPercentage >= 70.0 -> Color(0xFF1565C0)
        else -> Color(0xFFE65100)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = perf.salesPersonName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Badge(containerColor = contentColor.copy(alpha = 0.15f)) {
                    Text(text = statusText, color = contentColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Explanation Box
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = containerColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Penjelasan Status:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = contentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = statusExplanation, style = MaterialTheme.typography.bodySmall, color = contentColor)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Rincian Angka Kinerja:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Target Omset", CurrencyUtil.formatRupiah(perf.targetOmset))
                DetailRow("Omset Realisasi", CurrencyUtil.formatRupiah(perf.totalOmset))
                val shortfallAmount = (perf.targetOmset - perf.totalOmset).coerceAtLeast(0.0)
                DetailRow("Kekurangan Target", if (shortfallAmount > 0) CurrencyUtil.formatRupiah(shortfallAmount) else "Rp 0 (Tercapai)")
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Omset LUNAS (DONE)", CurrencyUtil.formatRupiah(perf.omsetDone))
                DetailRow("Omset TEMPO (Piutang)", CurrencyUtil.formatRupiah(perf.omsetTempo))

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(text = "Omset Belum Terealisasi (Proses):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("Status SO / Antrean (${perf.unrealizedBreakdown.countSo} Nota)", CurrencyUtil.formatRupiah(perf.unrealizedBreakdown.omsetSo))
                DetailRow("Status ON PROSES (${perf.unrealizedBreakdown.countOnProses} Nota)", CurrencyUtil.formatRupiah(perf.unrealizedBreakdown.omsetOnProses))
                DetailRow("Status PENGIRIMAN (${perf.unrealizedBreakdown.countDalamPengiriman} Nota)", CurrencyUtil.formatRupiah(perf.unrealizedBreakdown.omsetDalamPengiriman))
                DetailRow("Total Belum Terealisasi (${perf.pendingOrdersCount} Nota)", CurrencyUtil.formatRupiah(perf.pendingNominal))
                DetailRow(
                    "Potensi Total Omset",
                    "${CurrencyUtil.formatRupiah(perf.potentialTotalOmset)} (${String.format(java.util.Locale.getDefault(), "%.1f", perf.potentialAchievementPercentage)}%)"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(text = "Rincian Omset Kategori Harga:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("Retail (R)", CurrencyUtil.formatRupiah(perf.categoryBreakdown.omsetRetail))
                DetailRow("Semi Grosir (SG)", CurrencyUtil.formatRupiah(perf.categoryBreakdown.omsetSemiGrosir))
                DetailRow("Grosir (G)", CurrencyUtil.formatRupiah(perf.categoryBreakdown.omsetGrosir))
                DetailRow("Partai (P)", CurrencyUtil.formatRupiah(perf.categoryBreakdown.omsetPartai))
                DetailRow("Total Omset Kategori", CurrencyUtil.formatRupiah(perf.categoryBreakdown.totalCategorizedOmset))

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Total Kunjungan", "${perf.totalVisits} Kunjungan")
                DetailRow("Toko Unik Dikunjungi", "${perf.uniqueOutletsVisited} Toko")
                DetailRow("Toko Baru Didaftarkan", "${perf.newCustomersCount} Toko Baru")
                DetailRow("Total Nota Order Input", "${perf.totalOrdersCount} Nota")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun UnrealizedOmsetCard(
    title: String,
    unrealizedBreakdown: UnrealizedOmsetBreakdown,
    realizedOmset: Double,
    targetOmset: Double,
    onClickDetail: () -> Unit = {}
) {
    val totalUnrealized = unrealizedBreakdown.totalUnrealizedOmset
    val totalCount = unrealizedBreakdown.totalUnrealizedCount
    val potentialTotal = realizedOmset + totalUnrealized
    val potentialPct = if (targetOmset > 0.0) (potentialTotal / targetOmset) * 100.0 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickDetail() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = Color(0xFF0288D1),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF01579B)
                    )
                }
                Badge(containerColor = Color(0xFF0288D1)) {
                    Text(
                        text = "$totalCount Nota",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = CurrencyUtil.formatRupiah(totalUnrealized),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0288D1)
            )
            Text(
                text = "Pesanan berstatus SO, On Proses, atau Dalam Pengiriman yang belum menjadi omset sah (DONE/TEMPO).",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF01579B).copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF0288D1).copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            UnrealizedStatusRow(
                label = "Status SO / Antrean",
                count = unrealizedBreakdown.countSo,
                amount = unrealizedBreakdown.omsetSo,
                badgeColor = Color(0xFF0288D1)
            )
            Spacer(modifier = Modifier.height(6.dp))
            UnrealizedStatusRow(
                label = "Status ON PROSES",
                count = unrealizedBreakdown.countOnProses,
                amount = unrealizedBreakdown.omsetOnProses,
                badgeColor = Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.height(6.dp))
            UnrealizedStatusRow(
                label = "Status DALAM PENGIRIMAN",
                count = unrealizedBreakdown.countDalamPengiriman,
                amount = unrealizedBreakdown.omsetDalamPengiriman,
                badgeColor = Color(0xFF1565C0)
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF0288D1).copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Potensi Total Omset (Realisasi + Proses)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF01579B)
                    )
                    if (targetOmset > 0.0) {
                        Text(
                            text = "Potensi Pencapaian: ${String.format(java.util.Locale.getDefault(), "%.1f", potentialPct)}% dari Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0288D1)
                        )
                    }
                }
                Text(
                    text = CurrencyUtil.formatRupiah(potentialTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF01579B)
                )
            }
        }
    }
}

@Composable
private fun UnrealizedStatusRow(
    label: String,
    count: Int,
    amount: Double,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF01579B)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Badge(containerColor = badgeColor.copy(alpha = 0.15f)) {
                Text(
                    text = "$count Nota",
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            text = CurrencyUtil.formatRupiah(amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF01579B)
        )
    }
}

@Composable
fun PriceCategoryBreakdownCard(
    title: String,
    breakdown: PriceCategoryBreakdown,
    isLoadingDetails: Boolean = false
) {
    val totalCatOmset = breakdown.totalCategorizedOmset

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isLoadingDetails) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PriceCategoryRow(
                code = "R",
                label = "Harga Retail",
                amount = breakdown.omsetRetail,
                total = totalCatOmset,
                badgeColor = Color(0xFF1565C0)
            )
            Spacer(modifier = Modifier.height(8.dp))
            PriceCategoryRow(
                code = "SG",
                label = "Harga Semi Grosir",
                amount = breakdown.omsetSemiGrosir,
                total = totalCatOmset,
                badgeColor = Color(0xFF00838F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            PriceCategoryRow(
                code = "G",
                label = "Harga Grosir",
                amount = breakdown.omsetGrosir,
                total = totalCatOmset,
                badgeColor = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(8.dp))
            PriceCategoryRow(
                code = "P",
                label = "Harga Partai",
                amount = breakdown.omsetPartai,
                total = totalCatOmset,
                badgeColor = Color(0xFF6A1B9A)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL OMSET KATEGORI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CurrencyUtil.formatRupiah(totalCatOmset),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PriceCategoryRow(
    code: String,
    label: String,
    amount: Double,
    total: Double,
    badgeColor: Color
) {
    val pct = if (total > 0.0) (amount / total) * 100.0 else 0.0
    val progress = (pct / 100.0).coerceIn(0.0, 1.0).toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(containerColor = badgeColor.copy(alpha = 0.15f)) {
                    Text(
                        text = code,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${String.format(java.util.Locale.getDefault(), "%.1f", pct)}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyUtil.formatRupiah(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = badgeColor,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
