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
    @SerialName("remote_id") @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "sales_person_id") val salesPersonId: String,
    @ColumnInfo(name = "customer_id") val customerId: String,
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    @ColumnInfo(name = "order_date") val orderDate: String,
    val notes: String? = null,
    val details: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Double = 0.0,
    val status: String = "PENDING", // PENDING, PROCESSED, APPROVED, REJECTED
    @ColumnInfo(name = "admin_notes") val adminNotes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SalesOrderItem(
    @SerialName("product_id") val product_id: String,
    @SerialName("product_name") val product_name: String = "",
    @SerialName("qty") val quantity: Int,
    @SerialName("unit_price") val price: Double
)
