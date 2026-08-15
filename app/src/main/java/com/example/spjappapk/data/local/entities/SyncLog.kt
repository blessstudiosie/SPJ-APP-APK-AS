package com.example.spjappapk.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val event: String,
    val status: String, // "SUCCESS", "ERROR", "INFO"
    val message: String
)
