package com.example.spjappapk.util

import com.example.spjappapk.data.local.entities.*
import com.example.spjappapk.data.local.models.CompanyPerformanceSummary
import com.example.spjappapk.data.local.models.PriceCategoryBreakdown
import com.example.spjappapk.data.local.models.SalesPersonPerformance
import com.example.spjappapk.data.local.models.UnrealizedOmsetBreakdown
import com.example.spjappapk.ui.auth.SalesPerson
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale

object PerformanceCalculator {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val pendingStatuses = setOf("SO", "ON PROSES", "DALAM PENGIRIMAN", "PENDING", "PROCESSED")

    fun calculatePerformance(
        salesMasterList: List<SalesMaster>,
        visitsList: List<Visit>,
        customersList: List<Customer>,
        salesPersonEntities: List<SalesPersonEntity>,
        currentUser: SalesPerson?,
        targetMonth: Int,
        targetYear: Int,
        salesDetailsList: List<SalesDetailEntity> = emptyList(),
        localQueueOrders: List<SalesOrder> = emptyList()
    ): CompanyPerformanceSummary {
        // 1. Filter active sales (not soft-deleted)
        val activeSalesMaster = salesMasterList.filter { it.isDeleted != true }

        // 2. Filter sales in target month & year (DeliveryDate fallback OrderDate)
        val monthlySales = activeSalesMaster.filter { sale ->
            if (!BusinessRevenueHelper.isValidOmsetStatus(sale.status)) return@filter false
            
            val effTimestamp = sale.deliveryDate ?: sale.orderDate
            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+08:00")).apply {
                timeInMillis = effTimestamp
            }
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            m == targetMonth && y == targetYear
        }

        // 2b. Filter active unrealized (pending) sales in SalesMaster + unmerged local SalesOrder queue
        val pendingMasterSales = activeSalesMaster.filter { sale ->
            sale.status.trim().uppercase(Locale.ROOT) in pendingStatuses
        }
        val activeMasterRemoteIds = pendingMasterSales.map { it.id }.toSet()
        val unmergedLocalPending = localQueueOrders.filter { order ->
            order.status.trim().uppercase(Locale.ROOT) in pendingStatuses &&
            (order.remoteId.isNullOrBlank() || !activeMasterRemoteIds.contains(order.remoteId))
        }

        val detailsBySaleId = salesDetailsList.groupBy { it.saleId }

        // 3. Filter visits in target month & year (Deduplicated first)
        val deduplicatedVisitsList = visitsList.deduplicateVisits()
        val monthlyVisits = deduplicatedVisitsList.filter { visit ->
            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+08:00")).apply {
                timeInMillis = visit.timestamp
            }
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            m == targetMonth && y == targetYear
        }

        // 4. Calculate per sales person performance
        // Map staff targets
        val staffTargets = salesPersonEntities.associate { it.id to (it.targetOmset ?: 0.0) }
        val staffNames = salesPersonEntities.associate { it.id to it.name }

        // Group sales by resolved sales person ID or Name
        val allStaffIds = (
            salesPersonEntities.map { it.id } +
            monthlySales.mapNotNull { it.salesPersonId } +
            pendingMasterSales.mapNotNull { it.salesPersonId } +
            unmergedLocalPending.map { it.salesPersonId }.filter { it.isNotBlank() }
        ).distinct()

        val breakdown = mutableListOf<SalesPersonPerformance>()

        allStaffIds.forEach { staffId ->
            val staffName = staffNames[staffId] 
                ?: monthlySales.firstOrNull { it.salesPersonId == staffId }?.salesName
                ?: pendingMasterSales.firstOrNull { it.salesPersonId == staffId }?.salesName
                ?: unmergedLocalPending.firstOrNull { it.salesPersonId == staffId }?.salesPersonName
                ?: "Sales $staffId"

            val staffSales = monthlySales.filter { sale ->
                sale.salesPersonId?.equals(staffId, ignoreCase = true) == true ||
                sale.salesName?.equals(staffName, ignoreCase = true) == true
            }

            val staffPendingMaster = pendingMasterSales.filter { sale ->
                sale.salesPersonId?.equals(staffId, ignoreCase = true) == true ||
                sale.salesName?.equals(staffName, ignoreCase = true) == true
            }

            val staffPendingLocal = unmergedLocalPending.filter { order ->
                order.salesPersonId.equals(staffId, ignoreCase = true) ||
                order.salesPersonName.equals(staffName, ignoreCase = true)
            }

            val staffUnrealizedBreakdown = calculateUnrealizedBreakdown(staffPendingMaster, staffPendingLocal)
            val staffDone = staffSales.filter { it.status.trim().uppercase() == "DONE" }.sumOf { it.totalAmount }
            val staffTempo = staffSales.filter { it.status.trim().uppercase() == "TEMPO" }.sumOf { it.totalAmount }
            val staffTotalOmset = staffDone + staffTempo
            val staffCategoryBreakdown = calculateCategoryBreakdown(staffSales, detailsBySaleId)

            val staffVisits = monthlyVisits.filter { visit ->
                visit.userId.equals(staffId, ignoreCase = true) ||
                visit.salesPersonName.equals(staffName, ignoreCase = true)
            }

            val uniqueOutlets = staffVisits.map { it.customerId }.distinct().size
            
            val staffNewCustomers = customersList.count { cust ->
                cust.salesPersonId?.equals(staffId, ignoreCase = true) == true ||
                cust.salesPersonName?.equals(staffName, ignoreCase = true) == true
            }

            val target = staffTargets[staffId] ?: (if (staffId == currentUser?.id) currentUser.targetOmset ?: 0.0 else 0.0)

            if (staffTotalOmset > 0 || staffUnrealizedBreakdown.totalUnrealizedOmset > 0 || staffVisits.isNotEmpty() || target > 0) {
                breakdown.add(
                    SalesPersonPerformance(
                        salesPersonId = staffId,
                        salesPersonName = staffName,
                        targetOmset = target,
                        totalOmset = staffTotalOmset,
                        omsetDone = staffDone,
                        omsetTempo = staffTempo,
                        pendingNominal = staffUnrealizedBreakdown.totalUnrealizedOmset,
                        pendingOrdersCount = staffUnrealizedBreakdown.totalUnrealizedCount,
                        unrealizedBreakdown = staffUnrealizedBreakdown,
                        totalVisits = staffVisits.size,
                        uniqueOutletsVisited = uniqueOutlets,
                        newCustomersCount = staffNewCustomers,
                        totalOrdersCount = staffSales.size,
                        categoryBreakdown = staffCategoryBreakdown
                    )
                )
            }
        }

        breakdown.sortByDescending { it.totalOmset }

        val totalCompanyOmset = breakdown.sumOf { it.totalOmset }
        val totalCompanyTarget = breakdown.sumOf { it.targetOmset }
        val totalCompanyVisits = monthlyVisits.size
        val totalNewCust = breakdown.sumOf { it.newCustomersCount }

        val companyMetrics = BusinessRevenueHelper.calculateDashboardSummary(
            salesList = salesMasterList,
            salesPersonList = salesPersonEntities,
            targetMonth = targetMonth,
            targetYear = targetYear,
            currentUser = null // Company wide
        )

        val companyCategoryBreakdown = calculateCategoryBreakdown(monthlySales, detailsBySaleId)
        val companyUnrealizedBreakdown = calculateUnrealizedBreakdown(pendingMasterSales, unmergedLocalPending)

        return CompanyPerformanceSummary(
            totalCompanyOmset = totalCompanyOmset,
            totalCompanyTarget = totalCompanyTarget,
            totalCompanyUnrealizedOmset = companyUnrealizedBreakdown.totalUnrealizedOmset,
            totalCompanyUnrealizedOrders = companyUnrealizedBreakdown.totalUnrealizedCount,
            unrealizedBreakdown = companyUnrealizedBreakdown,
            totalCompanyVisits = totalCompanyVisits,
            totalNewCustomers = totalNewCust,
            totalCompanyOrders = monthlySales.size,
            metrics = companyMetrics,
            categoryBreakdown = companyCategoryBreakdown,
            salesBreakdown = breakdown
        )
    }

    private fun calculateUnrealizedBreakdown(
        pendingMaster: List<SalesMaster>,
        pendingLocal: List<SalesOrder>
    ): UnrealizedOmsetBreakdown {
        var omsetSo = 0.0
        var countSo = 0
        var omsetOnProses = 0.0
        var countOnProses = 0
        var omsetPengiriman = 0.0
        var countPengiriman = 0

        for (sale in pendingMaster) {
            when (sale.status.trim().uppercase(Locale.ROOT)) {
                "ON PROSES" -> {
                    omsetOnProses += sale.totalAmount
                    countOnProses++
                }
                "DALAM PENGIRIMAN" -> {
                    omsetPengiriman += sale.totalAmount
                    countPengiriman++
                }
                else -> {
                    omsetSo += sale.totalAmount
                    countSo++
                }
            }
        }

        for (order in pendingLocal) {
            val amount = if (order.totalAmount > 0.0) {
                order.totalAmount
            } else {
                try {
                    val items = json.decodeFromString<List<SalesOrderItem>>(order.details)
                    items.sumOf { it.displayPrice * it.quantity }
                } catch (_: Exception) {
                    0.0
                }
            }
            when (order.status.trim().uppercase(Locale.ROOT)) {
                "ON PROSES" -> {
                    omsetOnProses += amount
                    countOnProses++
                }
                "DALAM PENGIRIMAN" -> {
                    omsetPengiriman += amount
                    countPengiriman++
                }
                else -> {
                    omsetSo += amount
                    countSo++
                }
            }
        }

        return UnrealizedOmsetBreakdown(
            omsetSo = omsetSo,
            countSo = countSo,
            omsetOnProses = omsetOnProses,
            countOnProses = countOnProses,
            omsetDalamPengiriman = omsetPengiriman,
            countDalamPengiriman = countPengiriman
        )
    }

    private fun calculateCategoryBreakdown(
        sales: List<SalesMaster>,
        detailsBySaleId: Map<String, List<SalesDetailEntity>>
    ): PriceCategoryBreakdown {
        var retail = 0.0
        var semiGrosir = 0.0
        var grosir = 0.0
        var partai = 0.0

        for (sale in sales) {
            val details = detailsBySaleId[sale.id]
            if (!details.isNullOrEmpty()) {
                for (item in details) {
                    val lineTotal = if (item.subtotal > 0.0) item.subtotal else (item.price * item.quantity)
                    when (item.priceCategory?.trim()?.uppercase()) {
                        "R", "RETAIL" -> retail += lineTotal
                        "SG", "SEMI GROSIR", "SEMI_GROSIR" -> semiGrosir += lineTotal
                        "G", "GROSIR" -> grosir += lineTotal
                        "P", "PARTAI" -> partai += lineTotal
                        else -> retail += lineTotal
                    }
                }
            }
        }

        return PriceCategoryBreakdown(
            omsetRetail = retail,
            omsetSemiGrosir = semiGrosir,
            omsetGrosir = grosir,
            omsetPartai = partai
        )
    }
}
