package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "sales_master")
@Serializable
@Parcelize
data class SalesMaster(
    @PrimaryKey
    val id: String,
    
    val nota: String,
    
    @SerialName("order_date")
    @ColumnInfo(name = "order_date")
    @Serializable(with = com.example.spjappapk.util.DateAsLongSerializer::class)
    val orderDate: Long,
    
    @SerialName("delivery_date")
    @ColumnInfo(name = "delivery_date")
    @Serializable(with = com.example.spjappapk.util.OptionalDateAsLongSerializer::class)
    val deliveryDate: Long? = null,
    
    @SerialName("customer_id")
    @ColumnInfo(name = "customer_id")
    val customerId: String? = null,
    
    @SerialName("customer_name")
    @ColumnInfo(name = "customer_name")
    val customerName: String? = "",
    
    @SerialName("sales_person_id")
    @ColumnInfo(name = "sales_person_id")
    val salesPersonId: String? = null,

    @SerialName("sales_person")
    @ColumnInfo(name = "sales_person_name")
    val salesName: String? = null,
    
    @SerialName("total")
    @ColumnInfo(name = "total_amount")
    val totalAmount: Double = 0.0,

    @SerialName("paid")
    val paid: Double = 0.0,

    @SerialName("remaining")
    val remaining: Double = 0.0,

    val description: String? = "",
    val status: String = "",

    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,

    @SerialName("is_checked_system2")
    @ColumnInfo(name = "is_checked_system2")
    val isCheckedSystem2: Boolean = false,

    @SerialName("is_deleted")
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean? = false,

    @SerialName("is_synced")
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true
) : Parcelable
