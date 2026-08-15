package com.example.spjappapk.ui.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: CustomerViewModel
) {
    val customers by viewModel.customers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Konsumen") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Cari nama toko, alamat, atau pemilik...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            )

            if (isRefreshing && customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada data konsumen")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(customers) { customer ->
                        CustomerItem(customer = customer)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerItem(customer: com.example.spjappapk.data.local.entities.Customer) {
    ListItem(
        headlineContent = { 
            Text(customer.name, fontWeight = FontWeight.Bold) 
        },
        supportingContent = {
            Column {
                Text(customer.address ?: "Alamat tidak tersedia", style = MaterialTheme.typography.bodyMedium)
                if (!customer.ownerName.isNullOrBlank()) {
                    Text("Pemilik: ${customer.ownerName}", style = MaterialTheme.typography.bodySmall)
                }
                if (!customer.phone.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text(customer.phone!!, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                if (!customer.deliveryRoute.isNullOrBlank()) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(customer.deliveryRoute!!, modifier = Modifier.padding(2.dp))
                    }
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (customer.creditLimit > 0) {
                    Text(
                        "Limit: Rp${String.format(Locale.getDefault(), "%,.0f", customer.creditLimit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (customer.syncStatus == "PENDING") {
                    Icon(Icons.Default.Sync, null, tint = Color(0xFFFFA500), modifier = Modifier.size(16.dp))
                }
            }
        }
    )
}
