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
}
