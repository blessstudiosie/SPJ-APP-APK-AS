package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "sales_details_cache")
@Serializable
data class SalesDetailEntity(
    @PrimaryKey
    val id: String,
    
    @SerialName("sale_id")
    @ColumnInfo(name = "sale_id")
    val saleId: String,
    
    @SerialName("product_id")
    @ColumnInfo(name = "product_id")
    val productId: String? = null,
    
    @SerialName("qty")
    val quantity: Double,
    
    val price: Double,
    
    @SerialName("price_category")
    @ColumnInfo(name = "price_category")
    val priceCategory: String? = "R",
    
    val subtotal: Double,
    
    @SerialName("item_name")
    @ColumnInfo(name = "item_name")
    val itemName: String? = null,
    
    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,
    
    val nota: String? = null,
    
    @SerialName("is_checked_system2")
    @ColumnInfo(name = "is_checked_system2")
    val isCheckedSystem2: Boolean = false
)
