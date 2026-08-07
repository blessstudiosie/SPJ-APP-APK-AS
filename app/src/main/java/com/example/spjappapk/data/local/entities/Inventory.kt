package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "inventory")
@Serializable
data class Inventory(
    @PrimaryKey
    @SerialName("id") 
    @ColumnInfo(name = "remote_id") 
    val remoteId: String, // UUID from Supabase "id" column
    
    val name: String,
    
    @SerialName("stock_quantity") 
    @ColumnInfo(name = "stock_quantity") 
    val stockQuantity: Int,
    
    val price: Double
)
