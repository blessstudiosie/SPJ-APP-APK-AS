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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.spjappapk.ui.inventory.InventoryScreen
import com.example.spjappapk.ui.inventory.InventoryViewModel
import com.example.spjappapk.ui.navigation.Route
import com.example.spjappapk.ui.sales.SalesLedgerScreen
import com.example.spjappapk.ui.sales.SalesViewModel
import com.example.spjappapk.ui.notifications.NotificationScreen
import com.example.spjappapk.ui.notifications.NotificationViewModel
import com.example.spjappapk.ui.sync.PendingSyncScreen
import com.example.spjappapk.ui.sync.PendingSyncViewModel
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
                    label = { Text("Notifikasi & Feedback") },
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
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val currentRoute = backStack.last()
                        Text(when(currentRoute) {
                            is Route.Dashboard -> "Dashboard"
                            is Route.Inventory -> "Inventory"
                            is Route.Sales -> "Sales"
                            is Route.Visits -> "Visits"
                            is Route.PendingSync -> "Antrean Sync"
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
                                currentUser
                            )
                        }
                        DashboardScreen(viewModel)
                    }
                    is Route.Inventory -> NavEntry(r) {
                        val viewModel: InventoryViewModel = viewModel {
                            InventoryViewModel(app.inventoryRepository)
                        }
                        InventoryScreen(viewModel)
                    }
                    is Route.Sales -> NavEntry(r) {
                        val viewModel: SalesViewModel = viewModel {
                            SalesViewModel(
                                app.salesRepository,
                                app.inventoryRepository,
                                app.customerRepository
                            )
                        }
                        SalesLedgerScreen(viewModel)
                    }
                    is Route.Visits -> NavEntry(r) {
                        val viewModel: VisitsViewModel = viewModel {
                            VisitsViewModel(app.visitRepository, app.customerRepository)
                        }
                        VisitsScreen(viewModel)
                    }
                    is Route.PendingSync -> NavEntry(r) {
                        val viewModel: PendingSyncViewModel = viewModel {
                            PendingSyncViewModel(app.salesRepository, app.visitRepository)
                        }
                        PendingSyncScreen(viewModel)
                    }
                    is Route.Notifications -> NavEntry(r) {
                        val viewModel: NotificationViewModel = viewModel {
                            NotificationViewModel(app.salesRepository)
                        }
                        NotificationScreen(viewModel)
                    }
                    else -> NavEntry(r) {
                        Text("Unknown Route")
                    }
                }
            }
        }
    }
}
