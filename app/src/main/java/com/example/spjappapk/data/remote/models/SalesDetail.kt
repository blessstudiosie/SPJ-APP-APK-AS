package com.example.spjappapk.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesDetail(
    val id: String,
    @SerialName("sale_id") val saleId: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("qty") val quantity: Double,
    val price: Double,
    @SerialName("price_category") val priceCategory: String? = "R",
    val subtotal: Double,
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val nota: String? = null,
    @SerialName("is_checked_system2") val isCheckedSystem2: Boolean = false
)
