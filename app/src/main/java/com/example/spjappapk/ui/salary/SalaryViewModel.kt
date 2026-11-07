package com.example.spjappapk.ui.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.data.remote.models.SalaryConfirmation
import com.example.spjappapk.data.remote.models.SalaryScheme
import com.example.spjappapk.data.remote.models.SalesSalaryCalculation
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.util.BusinessRevenueHelper
import com.example.spjappapk.util.DateTimeUtil
import com.example.spjappapk.data.local.entities.deduplicateVisits
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID

import com.example.spjappapk.data.local.entities.SalesPersonEntity
import com.example.spjappapk.data.remote.models.DailyVisitDetail

data class SalaryUiState(
    val isLoading: Boolean = false,
    val startMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val startYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val endMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val endYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val scheme: SalaryScheme? = null,
    val calculation: SalesSalaryCalculation? = null,
    val error: String? = null,
    val isManagerOrOwner: Boolean = false,
    val staffList: List<SalesPersonEntity> = emptyList(),
    val selectedStaffId: String = ""
) {
    val startDate: Long get() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"))
        cal.set(Calendar.YEAR, startYear)
        cal.set(Calendar.MONTH, startMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val endDate: Long get() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"))
        cal.set(Calendar.YEAR, endYear)
        cal.set(Calendar.MONTH, endMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}

class SalaryViewModel(
    private val app: SPJApplication,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalaryUiState())
    val uiState: StateFlow<SalaryUiState> = _uiState.asStateFlow()

    init {
        val role = currentUser?.role?.lowercase() ?: ""
        val isMgr = role in listOf("manager", "owner", "admin")
        _uiState.update { it.copy(isManagerOrOwner = isMgr, selectedStaffId = currentUser?.id ?: "") }

        viewModelScope.launch {
            if (isMgr) {
                app.database.salesPersonDao().getAllSalesPersons().collect { staff ->
                    _uiState.update { state -> 
                        val newSelectedId = if (state.selectedStaffId.isEmpty() && staff.isNotEmpty()) staff.first().id else state.selectedStaffId
                        state.copy(staffList = staff, selectedStaffId = newSelectedId)
                    }
                    loadData()
                }
            } else {
                loadData()
            }
        }
    }

    fun setMonthRange(startMonth: Int, startYear: Int, endMonth: Int, endYear: Int) {
        _uiState.update { 
            it.copy(
                startMonth = startMonth,
                startYear = startYear,
                endMonth = endMonth,
                endYear = endYear
            ) 
        }
        loadData()
    }

    fun setSelectedStaff(staffId: String) {
        _uiState.update { it.copy(selectedStaffId = staffId) }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _uiState.value
                val targetStaffId = state.selectedStaffId
                if (targetStaffId.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                val targetStaffName = state.staffList.find { it.id == targetStaffId }?.name ?: currentUser?.name ?: "Sales"
                
                val start = state.startDate
                val end = state.endDate
                
                val monthNames = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                val periodStr = if (state.startMonth == state.endMonth && state.startYear == state.endYear) {
                    "${monthNames[state.startMonth - 1]} ${state.startYear}"
                } else {
                    "${monthNames[state.startMonth - 1]} ${state.startYear} s/d ${monthNames[state.endMonth - 1]} ${state.endYear}"
                }

                // 1. Get Scheme
                val scheme = app.salaryRepository.getSalaryScheme(targetStaffId, targetStaffName) 
                    ?: SalaryScheme() // Fallback to empty default if nothing found

                // 2. Get existing confirmation
                val confirmation = app.salaryRepository.getSalaryConfirmation(targetStaffId, periodStr)

                // 3. Get Sales Data (DONE & TEMPO)
                val salesMasterList = app.salesRepository.allSalesMaster.firstOrNull() ?: emptyList()
                val activeSales = salesMasterList.filter { 
                    it.isDeleted != true && 
                    BusinessRevenueHelper.isValidOmsetStatus(it.status) &&
                    (it.salesPersonId?.equals(targetStaffId, ignoreCase = true) == true)
                }

                val monthlySales = activeSales.filter { sale ->
                    val effTimestamp = sale.deliveryDate ?: sale.orderDate
                    effTimestamp in start..end
                }

                var omsetRetail = 0.0
                var omsetSemiGrosir = 0.0
                var omsetGrosir = 0.0
                var omsetPartai = 0.0

                if (scheme.useCategoryCommission) {
                    for (sale in monthlySales) {
                        val details = app.salesRepository.getSalesDetails(sale.id)
                        for (item in details) {
                            val lineTotal = item.price * item.quantity
                            when (item.priceCategory?.uppercase()) {
                                "R" -> omsetRetail += lineTotal
                                "SG" -> omsetSemiGrosir += lineTotal
                                "G" -> omsetGrosir += lineTotal
                                "P" -> omsetPartai += lineTotal
                                else -> omsetRetail += lineTotal // Default fallback
                            }
                        }
                    }
                } else {
                    omsetRetail = monthlySales.sumOf { it.totalAmount }
                }

                val totalOmset = monthlySales.sumOf { it.totalAmount }
                
                val komisiRetail = omsetRetail * (scheme.commissionRetailPercent / 100.0)
                val komisiSG = omsetSemiGrosir * (scheme.commissionSemiGrosirPercent / 100.0)
                val komisiG = omsetGrosir * (scheme.commissionGrosirPercent / 100.0)
                val komisiP = omsetPartai * (scheme.commissionPartaiPercent / 100.0)
                val totalKomisiOmset = komisiRetail + komisiSG + komisiG + komisiP

                // 4. Get Visits Data
                val allVisits = app.visitRepository.allVisits.firstOrNull() ?: emptyList()
                val monthlyVisits = allVisits.filter { visit ->
                    visit.userId.equals(targetStaffId, ignoreCase = true)
                }.filter { visit ->
                    visit.timestamp in start..end
                }

                val deduplicatedVisits = monthlyVisits.deduplicateVisits()

                // Group by Day String "yyyy-MM-dd" in GMT+8
                val visitsByDay = deduplicatedVisits.groupBy { visit ->
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00")).apply {
                        timeInMillis = visit.timestamp
                    }
                    DateTimeUtil.getFormat("yyyy-MM-dd").format(cal.time)
                }

                var daysTier1Count = 0
                var daysTier2Count = 0
                var daysTransportClaim = 0
                
                val dailyVisitDetails = mutableListOf<DailyVisitDetail>()

                visitsByDay.forEach { (dateStr, dailyVisits) ->
                    val count = dailyVisits.size
                    var transport = false
                    var tier = "Tidak Ada"
                    var bonus = 0.0

                    if (scheme.enableDailyTransportClaim && count > 0) {
                        daysTransportClaim++
                        transport = true
                    }
                    if (scheme.useDailyVisitBonus) {
                        if (count >= scheme.dailyVisitTargetTier2) {
                            daysTier1Count++
                            daysTier2Count++
                            tier = "Tier 2"
                            bonus = scheme.dailyVisitBonusTier1 + scheme.dailyVisitBonusTier2
                        } else if (count >= scheme.dailyVisitTargetTier1) {
                            daysTier1Count++
                            tier = "Tier 1"
                            bonus = scheme.dailyVisitBonusTier1
                        }
                    }
                    
                    dailyVisitDetails.add(
                        DailyVisitDetail(
                            dateStr = dateStr,
                            visitCount = count,
                            tierReached = tier,
                            bonusEarned = bonus,
                            transportClaimed = transport
                        )
                    )
                }
                
                dailyVisitDetails.sortBy { it.dateStr }

                val bonusTier1Total = daysTier1Count * scheme.dailyVisitBonusTier1
                val bonusTier2Total = daysTier2Count * scheme.dailyVisitBonusTier2
                val totalBonusKunjungan = bonusTier1Total + bonusTier2Total

                val takeHomePay = scheme.baseSalary + totalKomisiOmset + totalBonusKunjungan

                val calculation = SalesSalaryCalculation(
                    periodMonth = periodStr,
                    baseSalary = scheme.baseSalary,
                    totalOmset = totalOmset,
                    omsetRetail = omsetRetail,
                    komisiRetail = komisiRetail,
                    omsetSemiGrosir = omsetSemiGrosir,
                    komisiSemiGrosir = komisiSG,
                    omsetGrosir = omsetGrosir,
                    komisiGrosir = komisiG,
                    omsetPartai = omsetPartai,
                    komisiPartai = komisiP,
                    totalKomisiOmset = totalKomisiOmset,
                    totalVisits = deduplicatedVisits.size,
                    daysTier1Count = daysTier1Count,
                    bonusTier1Total = bonusTier1Total,
                    daysTier2Count = daysTier2Count,
                    bonusTier2Total = bonusTier2Total,
                    totalHariBonus = daysTier1Count, // Assuming tier 1 count is the baseline for days they got bonus
                    totalBonusKunjungan = totalBonusKunjungan,
                    daysTransportClaim = daysTransportClaim,
                    takeHomePay = takeHomePay,
                    isConfirmed = confirmation != null,
                    confirmedAtText = confirmation?.confirmedAt,
                    dailyVisitDetails = dailyVisitDetails
                )

                _uiState.update { it.copy(
                    isLoading = false,
                    scheme = scheme,
                    calculation = calculation
                ) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun confirmSalary() {
        val state = _uiState.value
        val calc = state.calculation ?: return
        if (currentUser == null) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val confirmation = SalaryConfirmation(
                id = UUID.randomUUID().toString(),
                salesPersonId = state.selectedStaffId,
                salesPersonName = state.staffList.find { it.id == state.selectedStaffId }?.name ?: currentUser.name,
                periodMonth = calc.periodMonth,
                totalOmset = calc.totalOmset,
                totalKomisiOmset = calc.totalKomisiOmset,
                totalKunjungan = calc.totalVisits,
                totalBonusKunjungan = calc.totalBonusKunjungan,
                totalHariTransport = calc.daysTransportClaim,
                baseSalary = calc.baseSalary,
                takeHomePay = calc.takeHomePay,
                status = "CONFIRMED",
                confirmedByName = currentUser.name,
                confirmedAt = DateTimeUtil.getUtcIsoFormat().format(Date()),
                confirmedSource = "ANDROID"
            )
            
            val success = app.salaryRepository.confirmSalary(confirmation)
            if (success) {
                loadData()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Gagal mengkonfirmasi tanda terima gaji.") }
            }
        }
    }
}
