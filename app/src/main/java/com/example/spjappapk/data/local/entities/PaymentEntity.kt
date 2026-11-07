package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "payments")
@Serializable
@Parcelize
data class PaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    @SerialName("sale_id")
    @ColumnInfo(name = "sale_id")
    val saleId: String,
    
    @SerialName("payment_date")
    @ColumnInfo(name = "payment_date")
    @Serializable(with = com.example.spjappapk.util.DateAsLongSerializer::class)
    val paymentDate: Long,
    
    val amount: Double,
    
    @SerialName("payment_method")
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String, // CASH, TRANSFER
    
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    
    val notes: String? = null,
    
    @SerialName("is_synced")
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    @Serializable(with = com.example.spjappapk.util.DateAsLongSerializer::class)
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
