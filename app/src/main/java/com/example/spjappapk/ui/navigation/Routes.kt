package com.example.spjappapk.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Login : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Inventory : Route

    @Serializable
    data object Customers : Route

    @Serializable
    data object Sales : Route

    @Serializable
    data object Visits : Route

    @Serializable
    data object Dashboard : Route

    @Serializable
    data object PendingSync : Route

    @Serializable
    data object SyncLog : Route

    @Serializable
    data object Notifications : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object AddSalesOrder : Route
    
    @Serializable
    data object TeamTurnoverDetail : Route
    
    @Serializable
    data object ErrorReports : Route

    @Serializable
    data class Payments(val saleId: String) : Route

    @Serializable
    data object PerformanceReport : Route

    @Serializable
    data object BillingInvoicesDetail : Route

    @Serializable
    data object AllPayments : Route

    @Serializable
    data object SalaryEstimation : Route

    @Serializable
    data object SalaryConfirmation : Route

    @Serializable
    data object PendingOmzetQueue : Route

    @Serializable
    data object TodayVisitsDetail : Route
}
