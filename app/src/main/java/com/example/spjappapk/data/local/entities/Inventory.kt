package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "inventory")
@Serializable
@Parcelize
data class Inventory(
    @PrimaryKey
    @SerialName("id") 
    @ColumnInfo(name = "remote_id") 
    val remoteId: String, // UUID from Supabase "id" column
    
    val name: String,
    
    @SerialName("stok_ready") 
    @ColumnInfo(name = "stok_ready") 
    val stockQuantity: Double = 0.0,
    
    @SerialName("harga_r") 
    @ColumnInfo(name = "harga_r") 
    val priceR: Double = 0.0,

    @SerialName("harga_sg")
    @ColumnInfo(name = "harga_sg")
    val priceSG: Double = 0.0,

    @SerialName("harga_g")
    @ColumnInfo(name = "harga_g")
    val priceG: Double = 0.0,

    @SerialName("harga_p")
    @ColumnInfo(name = "harga_p")
    val priceP: Double = 0.0,

    @SerialName("kategori")
    val category: String? = "Umum",

    @SerialName("satuan")
    val unit: String? = "PCS",

    @SerialName("status")
    val status: String? = "Y",

    @SerialName("stok_fisik")
    @ColumnInfo(name = "stok_fisik")
    val stockPhysical: Double = 0.0,

    @SerialName("satuan_besar")
    @ColumnInfo(name = "satuan_besar")
    val unitLarge: String? = "DUS",

    @SerialName("qty_ratio")
    @ColumnInfo(name = "qty_ratio")
    val qtyRatio: Double = 1.0,

    @SerialName("description")
    @ColumnInfo(name = "description")
    val description: String? = "",

    @SerialName("price_updated_at")
    @ColumnInfo(name = "price_updated_at")
    val priceUpdatedAt: String? = null,

    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,

    @SerialName("is_deleted")
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
) : Parcelable
