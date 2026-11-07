package com.example.spjappapk.util

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtil {
    // Menggunakan GMT+8 secara eksplisit untuk menghindari bug typo "Asia/Makasar" di sistem OS
    private val WITA_ZONE = TimeZone.getTimeZone("GMT+08:00")

    fun getFormat(pattern: String): SimpleDateFormat {
        return SimpleDateFormat(pattern, Locale.ROOT).apply {
            timeZone = WITA_ZONE
        }
    }

    /**
     * Get start of day in WITA (00:00:00.000)
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(WITA_ZONE).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Get end of day in WITA (23:59:59.999)
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(WITA_ZONE).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Get format for ISO 8601 in UTC (e.g. 2023-01-01T10:00:00Z)
     * Using UTC is more robust for server synchronization
     */
    fun getUtcIsoFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }
    
    /**
     * Format for database that is neutral to names
     */
    fun getDbFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    /**
     * Get format for ISO 8601 with timezone offset (e.g. 2023-01-01T10:00:00+08:00)
     */
    fun getIsoFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).apply {
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
