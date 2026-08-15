package com.example.spjappapk.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "sales_persons_profile")
@Serializable
@Parcelize
data class SalesPersonEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val role: String? = null,
    val phone: String? = "",
    val email: String? = "",
    @SerialName("target_omset")
    val targetOmset: Double? = 0.0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    @androidx.room.ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,
    val password: String? = null
) : Parcelable
