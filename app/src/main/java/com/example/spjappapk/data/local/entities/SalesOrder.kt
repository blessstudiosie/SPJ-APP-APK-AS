package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "sales_orders")
@Serializable
data class SalesOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sales_person_id") val salesPersonId: String, // UUID
    @ColumnInfo(name = "customer_id") val customerId: String, // UUID
    @ColumnInfo(name = "order_date") val orderDate: String, // YYYY-MM-DD
    val notes: String? = null,
    val details: String, // JSON String of List<SalesOrderItem>
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SalesOrderItem(
    val product_id: String, // UUID
    val quantity: Int,
    val price: Double
)
