package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "sales_orders")
@Serializable
@Parcelize
data class SalesOrder(
    @PrimaryKey(autoGenerate = true)
    @kotlinx.serialization.Transient
    val id: Long = 0,
    @SerialName("remote_id") @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "sales_person_id") val salesPersonId: String,
    @ColumnInfo(name = "sales_person_name") val salesPersonName: String = "",
    @ColumnInfo(name = "customer_id") val customerId: String,
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    @ColumnInfo(name = "order_date") val orderDate: String,
    val notes: String? = null,
    val details: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Double = 0.0,
    val status: String = "PENDING", // PENDING, PROCESSED, APPROVED, REJECTED
    @ColumnInfo(name = "admin_notes") val adminNotes: String? = null,
    val timestamp: Long = System.currentTimeMillis(),

    @SerialName("processed_at") @ColumnInfo(name = "processed_at") val processedAt: String? = null,
    @SerialName("processed_by") @ColumnInfo(name = "processed_by") val processedBy: String? = null,
    @SerialName("rejection_reason") @ColumnInfo(name = "rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: String? = null
) : Parcelable

@Serializable
@Parcelize
data class SalesOrderItem(
    @SerialName("product_id") val productId: String,
    @SerialName("item_name") val itemName: String = "",
    @SerialName("product_name") val productNameFallback: String? = null, // Handle old key
    @SerialName("qty") val quantity: Double = 0.0,
    @SerialName("price") val price: Double = 0.0,
    @SerialName("unit_price") val unitPriceFallback: Double? = null, // Handle old key
    @SerialName("price_category") val priceCategory: String = "R"
) : Parcelable {
    val displayProductName: String get() = itemName.ifBlank { productNameFallback ?: "Produk" }
    val displayPrice: Double get() = if (price > 0) price else unitPriceFallback ?: 0.0
}
