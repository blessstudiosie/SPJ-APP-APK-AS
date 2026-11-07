package com.example.spjappapk.ui.sales

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.spjappapk.util.CurrencyUtil
import com.example.spjappapk.util.DateTimeUtil
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFormDialog(
    maxAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, notes: String?, date: Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }
    var expandedMethod by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    var showDatePicker by remember { mutableStateOf(false) }

    val rawAmountString = amountText.replace(".", "")
    val amount = rawAmountString.toDoubleOrNull() ?: 0.0
    val isAmountValid = amount > 0 && amount <= maxAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Pembayaran Baru") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sisa: ${CurrencyUtil.formatRupiah(maxAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    AssistChip(
                        onClick = {
                            val cleanAmount = if (maxAmount % 1.0 == 0.0) maxAmount.toLong() else maxAmount.toLong()
                            amountText = String.format("%,d", cleanAmount).replace(',', '.')
                        },
                        label = { Text("Bayar Lunas", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        if (clean.isEmpty()) {
                            amountText = ""
                        } else {
                            val parsed = clean.toLongOrNull()
                            if (parsed != null) {
                                amountText = String.format("%,d", parsed).replace(',', '.')
                            }
                        }
                    },
                    label = { Text("Nominal Bayar (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountText.isNotEmpty() && !isAmountValid,
                    trailingIcon = {
                        if (amountText.isEmpty() || amount != maxAmount) {
                            TextButton(onClick = {
                                val cleanAmount = if (maxAmount % 1.0 == 0.0) maxAmount.toLong() else maxAmount.toLong()
                                amountText = String.format("%,d", cleanAmount).replace(',', '.')
                            }) {
                                Text("Lunas", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    supportingText = {
                        if (amountText.isNotEmpty() && amount > maxAmount) {
                            Text("Nominal melebihi sisa tagihan")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedMethod,
                    onExpandedChange = { expandedMethod = it }
                ) {
                    OutlinedTextField(
                        value = selectedMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metode Pembayaran") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMethod) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMethod,
                        onDismissRequest = { expandedMethod = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("CASH") },
                            onClick = { selectedMethod = "CASH"; expandedMethod = false }
                        )
                        DropdownMenuItem(
                            text = { Text("TRANSFER") },
                            onClick = { selectedMethod = "TRANSFER"; expandedMethod = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val date = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val dateStr = DateTimeUtil.formatDateOnly(date)

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tanggal Pembayaran") },
                    trailingIcon = { 
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is PressInteraction.Release) {
                                        showDatePicker = true
                                    }
                                }
                            }
                        }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        amount,
                        selectedMethod,
                        notes.ifBlank { null },
                        datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    )
                },
                enabled = isAmountValid
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
