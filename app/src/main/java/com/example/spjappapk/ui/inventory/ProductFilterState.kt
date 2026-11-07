package com.example.spjappapk.ui.inventory

enum class SortOption(val label: String) {
    NAME_ASC("Nama (A-Z)"),
    NAME_DESC("Nama (Z-A)"),
    PRICE_LOW_HIGH("Harga (Rendah → Tinggi)"),
    PRICE_HIGH_LOW("Harga (Tinggi → Rendah)"),
    STOCK_HIGH_LOW("Stok (Banyak → Sedikit)")
}

data class ProductFilterState(
    val selectedCategory: String? = null, // null = Semua
    val onlyStockReady: Boolean = false,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sortBy: SortOption = SortOption.NAME_ASC
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (!selectedCategory.isNullOrBlank() && selectedCategory != "Semua") count++
            if (onlyStockReady) count++
            if (minPrice != null) count++
            if (maxPrice != null) count++
            if (sortBy != SortOption.NAME_ASC) count++
            return count
        }

    val isFilterActive: Boolean get() = activeFilterCount > 0
}
