package com.example.spjappapk.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavEntry
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.ui.auth.AuthViewModel
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.ui.dashboard.DashboardScreen
import com.example.spjappapk.ui.dashboard.DashboardViewModel
import com.example.spjappapk.ui.error.ErrorReportsScreen
import com.example.spjappapk.ui.error.ErrorReportViewModel
import com.example.spjappapk.ui.inventory.InventoryScreen
import com.example.spjappapk.ui.inventory.InventoryViewModel
import com.example.spjappapk.ui.navigation.Route
import com.example.spjappapk.ui.sales.SalesLedgerScreen
import com.example.spjappapk.ui.sales.SalesViewModel
import com.example.spjappapk.ui.notifications.NotificationScreen
import com.example.spjappapk.ui.notifications.NotificationViewModel
import com.example.spjappapk.ui.sync.PendingSyncScreen
import com.example.spjappapk.ui.sync.PendingSyncViewModel
import com.example.spjappapk.ui.sync.SyncLogScreen
import com.example.spjappapk.ui.sync.SyncLogViewModel
import com.example.spjappapk.ui.visits.VisitsScreen
import com.example.spjappapk.ui.visits.VisitsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: SalesPerson?,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SPJApplication
    
    val backStack = rememberNavBackStack(Route.Dashboard)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SPJ Sales App",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentUser?.name ?: "User",
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                
                val currentRoute = backStack.last()
                val unreadCount by app.database.notificationDao().getUnreadCount().collectAsState(initial = 0)
                
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = currentRoute == Route.Dashboard,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.Dashboard) {
                            backStack.clear()
                            backStack.add(Route.Dashboard)
                        }
                    },
                    icon = { Icon(Icons.Default.Dashboard, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Inventory (Stok)") },
                    selected = currentRoute == Route.Inventory,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.Inventory) {
                            backStack.clear()
                            backStack.add(Route.Inventory)
                        }
                    },
                    icon = { Icon(Icons.Default.Inventory, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Daftar Konsumen") },
                    selected = currentRoute == Route.Customers,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.Customers) {
                            backStack.clear()
                            backStack.add(Route.Customers)
                        }
                    },
                    icon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Penjualan (Sales)") },
                    selected = currentRoute == Route.Sales,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.Sales) {
                            backStack.clear()
                            backStack.add(Route.Sales)
                        }
                    },
                    icon = { Icon(Icons.Default.History, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Kunjungan Lapangan") },
                    selected = currentRoute == Route.Visits,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.Visits) {
                            backStack.clear()
                            backStack.add(Route.Visits)
                        }
                    },
                    icon = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Antrean Sync") },
                    selected = currentRoute == Route.PendingSync,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.PendingSync) {
                            backStack.clear()
                            backStack.add(Route.PendingSync)
                        }
                    },
                    icon = { Icon(Icons.Default.Sync, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Log Sinkronisasi") },
                    selected = currentRoute == Route.SyncLog,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.SyncLog) {
                            backStack.clear()
                            backStack.add(Route.SyncLog)
                        }
                    },
                    icon = { Icon(Icons.Default.History, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Pengaturan") },
                    selected = currentRoute == Route.Settings,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.Settings) {
                            backStack.clear()
                            backStack.add(Route.Settings)
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notifikasi & Feedback", modifier = Modifier.weight(1f))
                            if (unreadCount > 0) {
                                Badge { Text("$unreadCount") }
                            }
                        }
                    },
                    selected = currentRoute == Route.Notifications,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.Notifications) {
                            backStack.clear()
                            backStack.add(Route.Notifications)
                        }
                    },
                    icon = { Icon(Icons.Default.Notifications, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Lapor Masalah / Error") },
                    selected = currentRoute == Route.ErrorReports,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentRoute != Route.ErrorReports) {
                            backStack.clear()
                            backStack.add(Route.ErrorReports)
                        }
                    },
                    icon = { Icon(Icons.Default.ReportProblem, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    icon = { Icon(Icons.Default.Logout, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "v${com.example.spjappapk.BuildConfig.VERSION_NAME} (${com.example.spjappapk.BuildConfig.BUILD_TIME})",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val currentRoute = backStack.last()
                val unreadCount by app.database.notificationDao().getUnreadCount().collectAsState(initial = 0)
                        Text(when(currentRoute) {
                            is Route.Dashboard -> "Dashboard"
                            is Route.Inventory -> "Inventory"
                            is Route.Customers -> "Daftar Konsumen"
                            is Route.Sales -> "Sales"
                            is Route.Visits -> "Visits"
                            is Route.PendingSync -> "Antrean Sync"
                            is Route.SyncLog -> "Log Sinkronisasi"
                            is Route.ErrorReports -> "Laporan Error"
                            is Route.Settings -> "Pengaturan"
                            is Route.AddSalesOrder -> "Input Pesanan Baru"
                            is Route.TeamTurnoverDetail -> "Rincian Omzet"
                            else -> "SPJ App"
                        })
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Open Menu")
                        }
                    }
                )
            }
        ) { padding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(padding)
            ) { route ->
                val r = route as Route
                when (r) {
                    is Route.Dashboard -> NavEntry(r) {
                        val viewModel: DashboardViewModel = viewModel {
                            DashboardViewModel(
                                app.salesRepository, 
                                app.visitRepository,
                                app.inventoryRepository,
                                app.customerRepository,
                                currentUser,
                                app
                            )
                        }
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSyncLog = {
                                backStack.clear()
                                backStack.add(Route.SyncLog)
                            },
                            onNavigateToVisits = {
                                backStack.clear()
                                backStack.add(Route.Visits)
                            },
                            onNavigateToNotifications = {
                                backStack.clear()
                                backStack.add(Route.Notifications)
                            },
                            onNavigateToTeamDetail = {
                                backStack.add(Route.TeamTurnoverDetail)
                            }
                        )
                    }
                    is Route.TeamTurnoverDetail -> NavEntry(r) {
                        val viewModel: DashboardViewModel = viewModel {
                            DashboardViewModel(
                                app.salesRepository, 
                                app.visitRepository,
                                app.inventoryRepository,
                                app.customerRepository,
                                currentUser,
                                app
                            )
                        }
                        com.example.spjappapk.ui.dashboard.TeamTurnoverScreen(
                            viewModel = viewModel,
                            onNavigateBack = { backStack.removeAt(backStack.size - 1) }
                        )
                    }
                    is Route.Inventory -> NavEntry(r) {
                        val viewModel: com.example.spjappapk.ui.inventory.InventoryViewModel = viewModel {
                            com.example.spjappapk.ui.inventory.InventoryViewModel(app.inventoryRepository)
                        }
                        com.example.spjappapk.ui.inventory.InventoryScreen(viewModel)
                    }
                    is Route.Customers -> NavEntry(r) {
                        val viewModel: com.example.spjappapk.ui.customers.CustomerViewModel = viewModel {
                            com.example.spjappapk.ui.customers.CustomerViewModel(app.customerRepository)
                        }
                        com.example.spjappapk.ui.customers.CustomerScreen(viewModel)
                    }
                    is Route.Sales -> NavEntry(r) {
                        val viewModel: SalesViewModel = viewModel {
                            SalesViewModel(
                                app,
                                app.salesRepository,
                                app.inventoryRepository,
                                app.customerRepository,
                                currentUser
                            )
                        }
                        SalesLedgerScreen(
                            viewModel = viewModel, 
                            currentSalesPersonName = currentUser?.name ?: "Unknown",
                            onNavigateToAddOrder = {
                                backStack.add(Route.AddSalesOrder)
                            }
                        )
                    }
                    is Route.AddSalesOrder -> NavEntry(r) {
                        // We can reuse or create a dedicated VM. For now, let's use the same one
                        // since it contains customers and products logic.
                         val viewModel: SalesViewModel = viewModel {
                            SalesViewModel(
                                app,
                                app.salesRepository,
                                app.inventoryRepository,
                                app.customerRepository,
                                currentUser
                            )
                        }
                        com.example.spjappapk.ui.sales.AddSalesOrderScreen(
                            viewModel = viewModel,
                            onNavigateBack = { backStack.removeAt(backStack.size - 1) }
                        )
                    }
                    is Route.Settings -> NavEntry(r) {
                        com.example.spjappapk.ui.settings.SettingsScreen(
                            onNavigateBack = {
                                backStack.clear()
                                backStack.add(Route.Dashboard)
                            }
                        )
                    }
                    is Route.Visits -> NavEntry(r) {
                        val viewModel: VisitsViewModel = viewModel {
                            VisitsViewModel(app.visitRepository, app.customerRepository, currentUser)
                        }
                        VisitsScreen(viewModel, currentUser?.name ?: "Unknown")
                    }
                    is Route.PendingSync -> NavEntry(r) {
                        val viewModel: PendingSyncViewModel = viewModel {
                            PendingSyncViewModel(app.salesRepository, app.visitRepository)
                        }
                        PendingSyncScreen(viewModel)
                    }
                    is Route.SyncLog -> NavEntry(r) {
                        SyncLogScreen(
                            onNavigateBack = {
                                backStack.clear()
                                backStack.add(Route.Dashboard)
                            }
                        )
                    }
                    is Route.Notifications -> NavEntry(r) {
                        val viewModel: NotificationViewModel = viewModel {
                            NotificationViewModel(app.notificationRepository, currentUser)
                        }
                        NotificationScreen(viewModel)
                    }
                    is Route.ErrorReports -> NavEntry(r) {
                        val viewModel: ErrorReportViewModel = viewModel {
                            ErrorReportViewModel(app)
                        }
                        ErrorReportsScreen(viewModel)
                    }
                    else -> NavEntry(r) {
                        Text("Unknown Route")
                    }
                }
            }
        }
    }
}
