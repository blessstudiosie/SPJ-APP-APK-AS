package com.example.spjappapk.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "notifications")
@Serializable
@Parcelize
data class Notification(
    @PrimaryKey
    val id: String,
    
    @SerialName("user_id")
    val userId: String?,
    
    val title: String,
    val message: String,
    val type: String = "INFO",
    
    @SerialName("is_read")
    val isRead: Boolean = false,
    
    @SerialName("created_at")
    val createdAt: String
) : Parcelable
