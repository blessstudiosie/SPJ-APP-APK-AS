package com.example.spjappapk.data.local.models

import com.example.spjappapk.util.DashboardMetrics

data class PriceCategoryBreakdown(
    val omsetRetail: Double = 0.0,
    val omsetSemiGrosir: Double = 0.0,
    val omsetGrosir: Double = 0.0,
    val omsetPartai: Double = 0.0
) {
    val totalCategorizedOmset: Double
        get() = omsetRetail + omsetSemiGrosir + omsetGrosir + omsetPartai
}

data class UnrealizedOmsetBreakdown(
    val omsetSo: Double = 0.0,
    val countSo: Int = 0,
    val omsetOnProses: Double = 0.0,
    val countOnProses: Int = 0,
    val omsetDalamPengiriman: Double = 0.0,
    val countDalamPengiriman: Int = 0
) {
    val totalUnrealizedOmset: Double
        get() = omsetSo + omsetOnProses + omsetDalamPengiriman

    val totalUnrealizedCount: Int
        get() = countSo + countOnProses + countDalamPengiriman
}

data class SalesPersonPerformance(
    val salesPersonId: String,
    val salesPersonName: String,
    val targetOmset: Double = 0.0,
    val totalOmset: Double = 0.0,
    val omsetDone: Double = 0.0,
    val omsetTempo: Double = 0.0,
    val pendingNominal: Double = 0.0,
    val pendingOrdersCount: Int = 0,
    val unrealizedBreakdown: UnrealizedOmsetBreakdown = UnrealizedOmsetBreakdown(),
    val totalVisits: Int = 0,
    val uniqueOutletsVisited: Int = 0,
    val newCustomersCount: Int = 0,
    val totalOrdersCount: Int = 0,
    val categoryBreakdown: PriceCategoryBreakdown = PriceCategoryBreakdown()
) {
    val achievementPercentage: Double
        get() = if (targetOmset > 0) (totalOmset / targetOmset) * 100.0 else 0.0

    val potentialTotalOmset: Double
        get() = totalOmset + pendingNominal

    val potentialAchievementPercentage: Double
        get() = if (targetOmset > 0) (potentialTotalOmset / targetOmset) * 100.0 else 0.0
}

data class CompanyPerformanceSummary(
    val totalCompanyOmset: Double = 0.0,
    val totalCompanyTarget: Double = 0.0,
    val totalCompanyUnrealizedOmset: Double = 0.0,
    val totalCompanyUnrealizedOrders: Int = 0,
    val unrealizedBreakdown: UnrealizedOmsetBreakdown = UnrealizedOmsetBreakdown(),
    val totalCompanyVisits: Int = 0,
    val totalNewCustomers: Int = 0,
    val totalCompanyOrders: Int = 0,
    val metrics: DashboardMetrics = DashboardMetrics(),
    val categoryBreakdown: PriceCategoryBreakdown = PriceCategoryBreakdown(),
    val salesBreakdown: List<SalesPersonPerformance> = emptyList()
) {
    val companyAchievementPercentage: Double
        get() = if (totalCompanyTarget > 0) (totalCompanyOmset / totalCompanyTarget) * 100.0 else 0.0

    val potentialCompanyOmset: Double
        get() = totalCompanyOmset + totalCompanyUnrealizedOmset

    val potentialCompanyAchievementPercentage: Double
        get() = if (totalCompanyTarget > 0) (potentialCompanyOmset / totalCompanyTarget) * 100.0 else 0.0
}
