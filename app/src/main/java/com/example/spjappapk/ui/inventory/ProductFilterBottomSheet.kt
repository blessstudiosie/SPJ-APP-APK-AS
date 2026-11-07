package com.example.spjappapk.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductFilterBottomSheet(
    initialFilterState: ProductFilterState,
    categories: List<String>,
    onDismiss: () -> Unit,
    onApplyFilter: (ProductFilterState) -> Unit,
    onResetFilter: () -> Unit
) {
    var tempCategory by remember { mutableStateOf(initialFilterState.selectedCategory) }
    var tempOnlyStockReady by remember { mutableStateOf(initialFilterState.onlyStockReady) }
    var tempMinPriceText by remember { mutableStateOf(initialFilterState.minPrice?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var tempMaxPriceText by remember { mutableStateOf(initialFilterState.maxPrice?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var tempSortBy by remember { mutableStateOf(initialFilterState.sortBy) }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Filter & Urutkan Produk",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Kategori Filter
            Text("Pilih Kategori", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = (tempCategory == null && cat == "Semua") || tempCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            tempCategory = if (cat == "Semua") null else cat
                        },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Stok Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hanya Stok Ready", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Sembunyikan produk dengan stok 0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = tempOnlyStockReady,
                    onCheckedChange = { tempOnlyStockReady = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Rentang Harga
            Text("Rentang Harga (Rp)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = tempMinPriceText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) tempMinPriceText = it },
                    label = { Text("Harga Min") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = tempMaxPriceText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) tempMaxPriceText = it },
                    label = { Text("Harga Maks") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 4. Pengurutan (Sorting)
            Text("Urutkan Berdasarkan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            SortOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = tempSortBy == option,
                        onClick = { tempSortBy = option }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = option.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onResetFilter()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Filter")
                }

                Button(
                    onClick = {
                        val minP = tempMinPriceText.toDoubleOrNull()
                        val maxP = tempMaxPriceText.toDoubleOrNull()
                        val newState = ProductFilterState(
                            selectedCategory = tempCategory,
                            onlyStockReady = tempOnlyStockReady,
                            minPrice = minP,
                            maxPrice = maxP,
                            sortBy = tempSortBy
                        )
                        onApplyFilter(newState)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Terapkan")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
