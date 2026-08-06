package com.example.spjappapk.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.entities.SalesOrder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(viewModel: NotificationViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifikasi & Feedback") }) }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada feedback dari admin.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(notifications) { order ->
                    NotificationItem(order = order)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun NotificationItem(order: SalesOrder) {
    val date = Date(order.timestamp)
    val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    val (icon, color, statusText) = when (order.status) {
        "APPROVED" -> Triple(Icons.Default.CheckCircle, Color(0xFF388E3C), "PESANAN DISETUJUI")
        "REJECTED" -> Triple(Icons.Default.Cancel, Color(0xFFD32F2F), "PESANAN DITOLAK")
        "PARTIALLY_APPROVED" -> Triple(Icons.Default.Info, Color(0xFFFBC02D), "DISETUJUI SEBAGIAN")
        else -> Triple(Icons.Default.Info, Color.Gray, order.status)
    }

    ListItem(
        leadingContent = { Icon(icon, null, tint = color) },
        headlineContent = { 
            Text(statusText, color = color, style = MaterialTheme.typography.labelLarge) 
        },
        supportingContent = {
            Column {
                Text("Order #${order.id} - Customer ID: ${order.customerId}")
                if (!order.adminNotes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Ket: ${order.adminNotes}",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = format.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    )
}
