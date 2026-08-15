package com.example.spjappapk.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "visits")
@Serializable
@Parcelize
data class Visit(
    @PrimaryKey(autoGenerate = true)
    @kotlinx.serialization.Transient
    val id: Long = 0,
    @SerialName("id") @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @SerialName("sales_person_id") @ColumnInfo(name = "user_id") val userId: String,
    @SerialName("sales_person") @ColumnInfo(name = "sales_person_name") val salesPersonName: String = "",
    @SerialName("customer_id") @ColumnInfo(name = "customer_id") val customerId: String = "",
    @SerialName("customer_name") @ColumnInfo(name = "customer_name") val customerName: String = "",
    @SerialName("is_new_customer") @ColumnInfo(name = "is_new_customer") val isNewCustomer: Boolean = false,
    @SerialName("latitude") @ColumnInfo(name = "location_lat") val locationLat: Double = 0.0,
    @SerialName("longitude") @ColumnInfo(name = "location_lng") val locationLng: Double = 0.0,
    @SerialName("photo_path") @ColumnInfo(name = "photo_path") val photoPath: String? = null,
    @SerialName("notes") @ColumnInfo(name = "notes") val notes: String? = null,
    @SerialName("check_in_date") @ColumnInfo(name = "check_in_date") val checkInDate: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("sync_status") @ColumnInfo(name = "sync_status") val syncStatus: String = "PENDING",
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: String? = null
) : Parcelable
