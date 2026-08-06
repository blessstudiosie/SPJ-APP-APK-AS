package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "sales_orders")
@Serializable
data class SalesOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @SerialName("remote_id") @ColumnInfo(name = "remote_id") val remoteId: String? = null, // UUID from queue
    @ColumnInfo(name = "sales_person_id") val salesPersonId: String,
    @ColumnInfo(name = "customer_id") val customerId: String,
    @ColumnInfo(name = "order_date") val orderDate: String,
    val notes: String? = null,
    val details: String,
    val status: String = "PENDING", // PENDING, PROCESSED (SENT), APPROVED, PARTIALLY_APPROVED, REJECTED
    @ColumnInfo(name = "admin_notes") val adminNotes: String? = null, // Feedback dari admin
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SalesOrderItem(
    val product_id: String, // UUID
    val quantity: Int,
    val price: Double
)
