package com.example.spjappapk.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.entities.Inventory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel) {
    val items by viewModel.inventoryItems.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<Inventory>()
    val coroutineScope = rememberCoroutineScope()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            InventoryList(
                items = items,
                onItemClick = { item ->
                    coroutineScope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                    }
                }
            )
        },
        detailPane = {
            val selectedItem = navigator.currentDestination?.contentKey
            if (selectedItem is Inventory) {
                InventoryDetail(item = selectedItem)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select an item to see details")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryList(
    items: List<Inventory>,
    onItemClick: (Inventory) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inventory") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text("Stock: ${item.stockQuantity}") },
                    trailingContent = { Text("$${item.price}") },
                    modifier = Modifier.clickable { onItemClick(item) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetail(item: Inventory) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(item.name) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = "Details", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Name: ${item.name}")
            Text(text = "Stock: ${item.stockQuantity}")
            Text(text = "Price: $${item.price}")
        }
    }
}
