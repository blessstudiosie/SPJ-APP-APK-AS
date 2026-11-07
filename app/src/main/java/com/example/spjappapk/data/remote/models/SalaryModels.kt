package com.example.spjappapk.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalaryScheme(
    val id: String = "",
    @SerialName("sales_person_id") val salesPersonId: String? = null,
    @SerialName("sales_person_name") val salesPersonName: String? = null,
    @SerialName("is_company_default") val isCompanyDefault: Boolean = false,
    @SerialName("base_salary") val baseSalary: Double = 0.0,
    @SerialName("use_category_commission") val useCategoryCommission: Boolean = true,
    @SerialName("commission_retail_percent") val commissionRetailPercent: Double = 7.0,
    @SerialName("commission_semi_grosir_percent") val commissionSemiGrosirPercent: Double = 5.0,
    @SerialName("commission_grosir_percent") val commissionGrosirPercent: Double = 2.0,
    @SerialName("commission_partai_percent") val commissionPartaiPercent: Double = 1.0,
    @SerialName("commission_general_percent") val commissionGeneralPercent: Double = 3.0,
    @SerialName("use_daily_visit_bonus") val useDailyVisitBonus: Boolean = true,
    @SerialName("daily_visit_target_tier1") val dailyVisitTargetTier1: Int = 10,
    @SerialName("daily_visit_bonus_tier1") val dailyVisitBonusTier1: Double = 20000.0,
    @SerialName("daily_visit_target_tier2") val dailyVisitTargetTier2: Int = 15,
    @SerialName("daily_visit_bonus_tier2") val dailyVisitBonusTier2: Double = 20000.0,
    @SerialName("enable_daily_transport_claim") val enableDailyTransportClaim: Boolean = true,
    @SerialName("transport_claim_description") val transportClaimDescription: String = "Bisa klaim uang transport",
    @SerialName("bonus_calculation_basis") val bonusCalculationBasis: String = "VISIT_COUNT",
    @SerialName("monthly_work_days") val monthlyWorkDays: Int = 25,
    @SerialName("always_grant_transport_claim") val alwaysGrantTransportClaim: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SalaryConfirmation(
    val id: String = "",
    @SerialName("sales_person_id") val salesPersonId: String = "",
    @SerialName("sales_person_name") val salesPersonName: String = "",
    @SerialName("period_month") val periodMonth: String = "",
    @SerialName("total_omset") val totalOmset: Double = 0.0,
    @SerialName("total_komisi_omset") val totalKomisiOmset: Double = 0.0,
    @SerialName("total_kunjungan") val totalKunjungan: Int = 0,
    @SerialName("total_bonus_kunjungan") val totalBonusKunjungan: Double = 0.0,
    @SerialName("total_hari_transport") val totalHariTransport: Int = 0,
    @SerialName("base_salary") val baseSalary: Double = 0.0,
    @SerialName("take_home_pay") val takeHomePay: Double = 0.0,
    val status: String = "PENDING",
    @SerialName("confirmed_by_user_id") val confirmedByUserId: String? = null,
    @SerialName("confirmed_by_name") val confirmedByName: String? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("confirmed_source") val confirmedSource: String? = null,
    val notes: String? = null,
    @SerialName("signature_image") val signatureImage: String? = null,
    @SerialName("requested_at") val requestedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

data class DailyVisitDetail(
    val dateStr: String,
    val visitCount: Int,
    val tierReached: String,
    val bonusEarned: Double,
    val transportClaimed: Boolean
)

data class SalesSalaryCalculation(
    val periodMonth: String,
    val baseSalary: Double,
    val totalOmset: Double,
    val omsetRetail: Double,
    val komisiRetail: Double,
    val omsetSemiGrosir: Double,
    val komisiSemiGrosir: Double,
    val omsetGrosir: Double,
    val komisiGrosir: Double,
    val omsetPartai: Double,
    val komisiPartai: Double,
    val totalKomisiOmset: Double,
    val totalVisits: Int,
    val daysTier1Count: Int,
    val bonusTier1Total: Double,
    val daysTier2Count: Int,
    val bonusTier2Total: Double,
    val totalHariBonus: Int,
    val totalBonusKunjungan: Double,
    val daysTransportClaim: Int,
    val takeHomePay: Double,
    val isConfirmed: Boolean,
    val confirmedAtText: String?,
    val dailyVisitDetails: List<DailyVisitDetail> = emptyList()
)
