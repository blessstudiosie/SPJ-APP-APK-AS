package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "customers")
@Serializable
@Parcelize
data class Customer(
    @PrimaryKey
    @SerialName("id") 
    @ColumnInfo(name = "remote_id") 
    val remoteId: String, // UUID from Supabase "id" column
    
    val name: String,

    @SerialName("owner_name")
    @ColumnInfo(name = "owner_name")
    val ownerName: String? = "",

    val address: String? = null,
    val phone: String? = null,
    
    val latitude: Double? = null,
    val longitude: Double? = null,

    @SerialName("sales_person_id")
    @ColumnInfo(name = "sales_person_id")
    val salesPersonId: String? = null,

    @SerialName("sales_person")
    @ColumnInfo(name = "sales_person_name")
    val salesPersonName: String? = null,

    @SerialName("jalur_pengiriman")
    @ColumnInfo(name = "jalur_pengiriman")
    val deliveryRoute: String? = "LOBAR-MATARAM",

    @SerialName("limit_piutang")
    @ColumnInfo(name = "limit_piutang")
    val creditLimit: Double = 0.0,

    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SUCCESS" // SUCCESS, PENDING
) : Parcelable
