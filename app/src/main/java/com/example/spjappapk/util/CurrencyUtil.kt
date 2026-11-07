package com.example.spjappapk.util

import java.util.Locale

object CurrencyUtil {
    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Double): String {
        return "Rp ${String.format(indonesianLocale, "%,.0f", amount)}"
    }
    
    fun formatRupiahNoPrefix(amount: Double): String {
        return String.format(indonesianLocale, "%,.0f", amount)
    }
}
