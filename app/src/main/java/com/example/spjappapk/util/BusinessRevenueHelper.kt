package com.example.spjappapk.util

import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.ui.auth.SalesPerson
import java.util.Calendar
import java.util.Locale

data class DashboardMetrics(
    val totalOmset: Double = 0.0,
    val totalNotaOmset: Int = 0,
    val omsetDone: Double = 0.0,
    val countDone: Int = 0,
    val omsetTempo: Double = 0.0,
    val countTempo: Int = 0,
    val pendingNominal: Double = 0.0,
    val pendingCount: Int = 0,
    val salesBreakdown: Map<String, Double> = emptyMap()
)

object BusinessRevenueHelper {

    fun isValidOmsetStatus(status: String?): Boolean {
        if (status.isNullOrBlank()) return false
        val clean = status.trim().uppercase(Locale.ROOT)
        return clean == "DONE" || clean == "TEMPO"
    }

    fun resolveSalesPersonName(
        sale: SalesMaster, 
        salesPersonList: List<com.example.spjappapk.data.local.entities.SalesPersonEntity>
    ): String {
        if (!sale.salesName.isNullOrBlank()) return sale.salesName
        if (!sale.salesPersonId.isNullOrBlank()) {
            val sp = salesPersonList.find { it.id.equals(sale.salesPersonId, ignoreCase = true) }
            if (sp != null && !sp.name.isNullOrBlank()) return sp.name
        }
        return "Lainnya"
    }

    /**
     * Menghitung ringkasan dashboard berdasarkan SOP SPJ.
     * @param targetMonth 1-12
     */
    fun calculateDashboardSummary(
        salesList: List<SalesMaster>,
        salesPersonList: List<com.example.spjappapk.data.local.entities.SalesPersonEntity>,
        targetMonth: Int, 
        targetYear: Int,
        currentUser: SalesPerson?
    ): DashboardMetrics {
        val isManagerOrOwner = currentUser?.role?.lowercase() in listOf("manager", "owner", "admin")
        val currentUserId = currentUser?.id?.trim() ?: ""
        val currentUserName = currentUser?.name?.trim() ?: ""

        // 1. Filter Soft-Delete
        var activeSales = salesList.filter { it.isDeleted != true }

        // 2. Filter Role SALES (Jika bukan Manager/Owner/Admin)
        if (!isManagerOrOwner) {
            activeSales = activeSales.filter { sale ->
                val resolvedName = resolveSalesPersonName(sale, salesPersonList)
                val matchId = currentUserId.isNotBlank() && (sale.salesPersonId?.equals(currentUserId, ignoreCase = true) == true)
                val matchName = currentUserName.isNotBlank() && (resolvedName.equals(currentUserName, ignoreCase = true))
                matchId || matchName
            }
        }

        // 3. Filter Bulan & Tahun berdasarkan DeliveryDate (Fallback ke OrderDate)
        val currentMonthDelivered = activeSales.filter { sale ->
            if (!isValidOmsetStatus(sale.status)) return@filter false
            
            // Di proyek ini, deliveryDate dan orderDate sudah berupa Long (timestamp)
            val effTimestamp = sale.deliveryDate ?: sale.orderDate

            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Makassar")).apply { 
                timeInMillis = effTimestamp 
            }
            val m = cal.get(Calendar.MONTH) + 1 // 1-12
            val y = cal.get(Calendar.YEAR)

            m == targetMonth && y == targetYear
        }

        val doneList = currentMonthDelivered.filter { it.status.trim().uppercase(Locale.ROOT) == "DONE" }
        val tempoList = currentMonthDelivered.filter { it.status.trim().uppercase(Locale.ROOT) == "TEMPO" }

        val omsetDone = doneList.sumOf { it.totalAmount }
        val omsetTempo = tempoList.sumOf { it.totalAmount }

        val pendingList = activeSales.filter {
            val st = it.status.trim().uppercase(Locale.ROOT)
            st == "SO" || st == "ON PROSES" || st == "DALAM PENGIRIMAN"
        }
        val pendingNominal = pendingList.sumOf { it.totalAmount }

        // Breakdown per Sales (untuk Manager/Owner)
        val breakdown = mutableMapOf<String, Double>()
        currentMonthDelivered.forEach { sale ->
            val name = resolveSalesPersonName(sale, salesPersonList)
            breakdown[name] = (breakdown[name] ?: 0.0) + sale.totalAmount
        }

        return DashboardMetrics(
            totalOmset = omsetDone + omsetTempo,
            totalNotaOmset = currentMonthDelivered.size,
            omsetDone = omsetDone,
            countDone = doneList.size,
            omsetTempo = omsetTempo,
            countTempo = tempoList.size,
            pendingNominal = pendingNominal,
            pendingCount = pendingList.size,
            salesBreakdown = breakdown
        )
    }
}
