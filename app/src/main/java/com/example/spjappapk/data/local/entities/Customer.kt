package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "customers")
@Serializable
data class Customer(
    @PrimaryKey
    @SerialName("id") 
    @ColumnInfo(name = "remote_id") 
    val remoteId: String, // UUID from Supabase "id" column
    
    val name: String,
    val address: String? = null,
    val phone: String? = null
)
