package com.example.spjappapk.util

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtil {
    private val WITA_ZONE = TimeZone.getTimeZone("Asia/Makassar")

    fun getFormat(pattern: String): SimpleDateFormat {
        return SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = WITA_ZONE
        }
    }

    /**
     * Get format for ISO 8601 with timezone offset (e.g. 2023-01-01T10:00:00+08:00)
     */
    fun getIsoFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
            timeZone = WITA_ZONE
        }
    }

    /**
     * Robust parsing for various date formats (ISO 8601, SQL, and Indonesian formats)
     */
    fun parseIso(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy",
            "d/M/yyyy"
        )

        for (pattern in patterns) {
            try {
                return SimpleDateFormat(pattern, Locale.ROOT).apply {
                    timeZone = WITA_ZONE
                }.parse(dateStr)
            } catch (e: Exception) {}
        }
        return null
    }

    /**
     * Get current date as string for database (yyyy-MM-dd)
     */
    fun getCurrentDateString(): String {
        return getFormat("yyyy-MM-dd").format(Date())
    }

    /**
     * Format timestamp to displayable string (Date only)
     */
    fun formatDateOnly(timestamp: Long): String {
        return getFormat("dd MMM yyyy").format(Date(timestamp))
    }

    /**
     * Format timestamp to displayable string
     */
    fun formatDisplay(timestamp: Long): String {
        return getFormat("dd MMM yyyy, HH:mm").format(Date(timestamp))
    }

    /**
     * Format for logs (shorter)
     */
    fun formatLog(timestamp: Long): String {
        return getFormat("dd/MM HH:mm:ss").format(Date(timestamp))
    }

    /**
     * Format for filenames
     */
    fun formatFileName(): String {
        return getFormat("yyyyMMdd_HHmmss").format(Date())
    }
}
