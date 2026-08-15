package com.example.spjappapk.ui.error

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorReportsScreen(
    viewModel: ErrorReportViewModel
) {
    val context = LocalContext.current
    val errorText = viewModel.errorText
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()
    var feedbackText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Laporan & Feedback",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kirim Feedback / Masukan", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Tuliskan kendala atau saran Anda di sini...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { 
                        viewModel.sendReport(feedbackText)
                        feedbackText = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading && feedbackText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kirim ke Google Drive")
                }
            }
        }

        if (uploadStatus != null) {
            Text(
                text = uploadStatus!!,
                color = if (uploadStatus!!.contains("Berhasil")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Laporan Masalah & Logs",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        if (errorText == null && viewModel.syncLogsText == null) {
            Text("Tidak ada laporan crash atau log sinkronisasi terbaru.", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
        } else {
            Card(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                    if (errorText != null) {
                        Text("--- CRASH LOG ---", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = errorText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (viewModel.syncLogsText != null) {
                        Text("--- RECENT SYNC LOGS ---", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = viewModel.syncLogsText!!, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.sendReport() },
                    modifier = Modifier.weight(1f),
                    enabled = !isUploading
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unggah Log")
                }
                
                OutlinedButton(
                    onClick = { viewModel.clearReport() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus")
                }
            }
        }
    }
}
