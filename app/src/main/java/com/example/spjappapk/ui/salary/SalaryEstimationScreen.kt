package com.example.spjappapk.ui.salary

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.remote.models.SalesSalaryCalculation
import com.example.spjappapk.util.CurrencyUtil
import com.example.spjappapk.util.DateTimeUtil
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryEstimationScreen(
    viewModel: SalaryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estimasi Gaji & Komisi") },
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
                    .verticalScroll(rememberScrollState())
            ) {
                MonthRangeSelector(
                    startMonth = uiState.startMonth,
                    startYear = uiState.startYear,
                    endMonth = uiState.endMonth,
                    endYear = uiState.endYear,
                    onMonthRangeSelected = { sMonth, sYear, eMonth, eYear ->
                        viewModel.setMonthRange(sMonth, sYear, eMonth, eYear)
                    }
                )

                if (uiState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (uiState.isManagerOrOwner && uiState.staffList.isNotEmpty()) {
                    var expandedStaff by remember { mutableStateOf(false) }
                    val currentStaffName = uiState.staffList.find { it.id == uiState.selectedStaffId }?.name ?: "Pilih Sales"

                    ExposedDropdownMenuBox(
                        expanded = expandedStaff,
                        onExpandedChange = { expandedStaff = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentStaffName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Sales Person") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStaff) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStaff,
                            onDismissRequest = { expandedStaff = false }
                        ) {
                            uiState.staffList.forEach { staff ->
                                val displayName = staff.name + if (!staff.isActive) " (Resign)" else ""
                                DropdownMenuItem(
                                    text = { Text(displayName) },
                                    onClick = {
                                        viewModel.setSelectedStaff(staff.id)
                                        expandedStaff = false
                                    }
                                )
                            }
                        }
                    }
                } else if (!uiState.isManagerOrOwner) {
                    val currentStaff = uiState.staffList.find { it.id == uiState.selectedStaffId }
                    val displayName = (currentStaff?.name ?: "Anda") + if (currentStaff?.isActive == false) " (Resign)" else ""
                    Text(
                        text = "Sales: $displayName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (uiState.calculation != null) {
                    val calc = uiState.calculation!!

                    // Overview Card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (calc.isConfirmed) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "Estimasi Take Home Pay",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (calc.isConfirmed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (calc.isConfirmed) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Confirmed", tint = Color(0xFF2E7D32))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = CurrencyUtil.formatRupiah(calc.takeHomePay),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (calc.isConfirmed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                            if (calc.isConfirmed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Badge(containerColor = Color(0xFF2E7D32)) {
                                    Text("Telah Dikonfirmasi (Sah)", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }

                    // Gaji Pokok
                    SectionHeader("Gaji Pokok")
                    DetailCard {
                        DetailRow("Base Salary", CurrencyUtil.formatRupiah(calc.baseSalary), isBold = true)
                    }

                    // Komisi Penjualan
                    SectionHeader("Komisi Penjualan (Omset)")
                    DetailCard {
                        if (uiState.scheme?.useCategoryCommission == true) {
                            DetailRow("Omset Retail (${uiState.scheme!!.commissionRetailPercent}%)", CurrencyUtil.formatRupiah(calc.omsetRetail))
                            DetailRow("➜ Komisi", CurrencyUtil.formatRupiah(calc.komisiRetail), Color(0xFF388E3C))
                            Spacer(Modifier.height(4.dp))
                            
                            DetailRow("Omset Semi Grosir (${uiState.scheme!!.commissionSemiGrosirPercent}%)", CurrencyUtil.formatRupiah(calc.omsetSemiGrosir))
                            DetailRow("➜ Komisi", CurrencyUtil.formatRupiah(calc.komisiSemiGrosir), Color(0xFF388E3C))
                            Spacer(Modifier.height(4.dp))

                            DetailRow("Omset Grosir (${uiState.scheme!!.commissionGrosirPercent}%)", CurrencyUtil.formatRupiah(calc.omsetGrosir))
                            DetailRow("➜ Komisi", CurrencyUtil.formatRupiah(calc.komisiGrosir), Color(0xFF388E3C))
                            Spacer(Modifier.height(4.dp))

                            DetailRow("Omset Partai (${uiState.scheme!!.commissionPartaiPercent}%)", CurrencyUtil.formatRupiah(calc.omsetPartai))
                            DetailRow("➜ Komisi", CurrencyUtil.formatRupiah(calc.komisiPartai), Color(0xFF388E3C))
                        } else {
                            DetailRow("Total Omset Sah", CurrencyUtil.formatRupiah(calc.totalOmset))
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("TOTAL KOMISI OMSET", CurrencyUtil.formatRupiah(calc.totalKomisiOmset), isBold = true)
                    }

                    // Bonus Kunjungan & Transport
                    SectionHeader("Kunjungan & Transport")
                    DetailCard {
                        var showDailyDetails by remember { mutableStateOf(false) }

                        DetailRow("Total Titik Kunjungan", "${calc.totalVisits} Titik")
                        
                        if (uiState.scheme?.useDailyVisitBonus == true) {
                            Spacer(Modifier.height(8.dp))
                            Text("Pencapaian Harian:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            DetailRow("Tier 1 (≥ ${uiState.scheme!!.dailyVisitTargetTier1} /hari)", "${calc.daysTier1Count} Hari")
                            DetailRow("➜ Bonus", CurrencyUtil.formatRupiah(calc.bonusTier1Total), Color(0xFF388E3C))
                            
                            DetailRow("Tier 2 (≥ ${uiState.scheme!!.dailyVisitTargetTier2} /hari)", "${calc.daysTier2Count} Hari")
                            DetailRow("➜ Bonus Tambahan", CurrencyUtil.formatRupiah(calc.bonusTier2Total), Color(0xFF388E3C))
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailRow("TOTAL BONUS KUNJUNGAN", CurrencyUtil.formatRupiah(calc.totalBonusKunjungan), isBold = true)
                        }

                        if (uiState.scheme?.enableDailyTransportClaim == true) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailRow("Hak Klaim Transport (Hari Kerja)", "${calc.daysTransportClaim} Hari", isBold = true)
                            Text(uiState.scheme!!.transportClaimDescription, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showDailyDetails = !showDailyDetails }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showDailyDetails) "Sembunyikan Rincian Harian" else "Lihat Rincian Harian", color = MaterialTheme.colorScheme.primary)
                        }

                        AnimatedVisibility(visible = showDailyDetails) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text("Rincian Harian:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                
                                if (calc.dailyVisitDetails.isEmpty()) {
                                    Text("Tidak ada kunjungan di periode ini.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                } else {
                                    calc.dailyVisitDetails.forEach { detail ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(detail.dateStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                Text("${detail.visitCount} Titik • ${detail.tierReached}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                            if (detail.bonusEarned > 0) {
                                                Text("+${CurrencyUtil.formatRupiah(detail.bonusEarned)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("Rp 0", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions
                    if (!calc.isConfirmed) {
                        Button(
                            onClick = { viewModel.confirmSalary() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)
                        ) {
                            Icon(Icons.Default.TaskAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Konfirmasi Tanda Terima Sah")
                        }
                        Text(
                            text = "* Dengan mengkonfirmasi, Anda menyetujui perhitungan di atas sesuai sistem SPJ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        OutlinedButton(
                            onClick = {
                                val text = buildShareText(calc)
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Bagikan Slip Gaji (WA)")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthRangeSelector(
    startMonth: Int,
    startYear: Int,
    endMonth: Int,
    endYear: Int,
    onMonthRangeSelected: (startMonth: Int, startYear: Int, endMonth: Int, endYear: Int) -> Unit
) {
    var pickerTarget by remember { mutableStateOf<String?>(null) }
    val monthNames = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

    val startText = "${monthNames[startMonth - 1]} $startYear"
    val endText = "${monthNames[endMonth - 1]} $endYear"

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Periode Bulan Estimasi:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { pickerTarget = "START" }, modifier = Modifier.weight(1f)) {
                Text(startText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Text("  s/d  ", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { pickerTarget = "END" }, modifier = Modifier.weight(1f)) {
                Text(endText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (pickerTarget == "START") {
        MonthYearPickerDialog(
            title = "Pilih Bulan Mulai",
            initialMonth = startMonth,
            initialYear = startYear,
            onDismiss = { pickerTarget = null },
            onConfirm = { m, y ->
                pickerTarget = null
                onMonthRangeSelected(m, y, endMonth, endYear)
            }
        )
    } else if (pickerTarget == "END") {
        MonthYearPickerDialog(
            title = "Pilih Bulan Akhir",
            initialMonth = endMonth,
            initialYear = endYear,
            onDismiss = { pickerTarget = null },
            onConfirm = { m, y ->
                pickerTarget = null
                onMonthRangeSelected(startMonth, startYear, m, y)
            }
        )
    }
}

@Composable
fun MonthYearPickerDialog(
    title: String,
    initialMonth: Int,
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (month: Int, year: Int) -> Unit
) {
    var tempMonth by remember { mutableIntStateOf(initialMonth) }
    var tempYear by remember { mutableIntStateOf(initialYear) }
    val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Year Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    IconButton(onClick = { tempYear-- }) {
                        Icon(Icons.Default.ChevronLeft, "Previous Year")
                    }
                    Text(text = "$tempYear", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { tempYear++ }) {
                        Icon(Icons.Default.ChevronRight, "Next Year")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 12 Months Grid
                Column {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..2) {
                                val monthIndex = row * 3 + col
                                val monthNum = monthIndex + 1
                                val isSelected = monthNum == tempMonth

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { tempMonth = monthNum },
                                    label = { Text(monthNames[monthIndex]) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempMonth, tempYear) }) {
                Text("Pilih Bulan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

fun buildShareText(calc: SalesSalaryCalculation): String {
    return """
        *SLIP GAJI & KOMISI SALES SPJ*
        Periode: ${calc.periodMonth}
        
        *PENDAPATAN*
        Gaji Pokok: ${CurrencyUtil.formatRupiah(calc.baseSalary)}
        Komisi Omset: ${CurrencyUtil.formatRupiah(calc.totalKomisiOmset)}
        Bonus Kunjungan: ${CurrencyUtil.formatRupiah(calc.totalBonusKunjungan)}
        ----------------------------
        *TAKE HOME PAY: ${CurrencyUtil.formatRupiah(calc.takeHomePay)}*
        
        *DETAIL KINERJA*
        Total Omset Sah: ${CurrencyUtil.formatRupiah(calc.totalOmset)}
        Total Kunjungan: ${calc.totalVisits} Titik
        Hak Transport: ${calc.daysTransportClaim} Hari Kerja
        
        Status: Disetujui secara digital via SPJ App
        Waktu: ${calc.confirmedAtText ?: "-"}
    """.trimIndent()
}
