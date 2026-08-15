package com.example.spjappapk.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spjappapk.data.local.entities.Notification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(viewModel: NotificationViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Notifikasi & Feedback") },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text("Tandai Semua Dibaca", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            ) 
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada notifikasi baru.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = { viewModel.markAsRead(notification.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    val dateText = com.example.spjappapk.util.DateTimeUtil.formatDisplay(notification.createdAt.toLongOrNull() ?: System.currentTimeMillis())

    val (icon, color) = when (notification.type) {
        "SUCCESS" -> Icons.Default.CheckCircle to Color(0xFF388E3C)
        "ERROR" -> Icons.Default.Cancel to Color(0xFFD32F2F)
        "WARNING" -> Icons.Default.Info to Color(0xFFFBC02D)
        else -> Icons.Default.Info to Color.Gray
    }

    ListItem(
        leadingContent = { 
            BadgedBox(badge = {
                if (!notification.isRead) {
                    Badge(modifier = Modifier.size(8.dp))
                }
            }) {
                Icon(icon, null, tint = if (notification.isRead) color.copy(alpha = 0.5f) else color)
            }
        },
        headlineContent = { 
            Text(
                text = notification.title, 
                color = if (notification.isRead) Color.Gray else color, 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
            ) 
        },
        supportingContent = {
            Column {
                Text(notification.message)
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
