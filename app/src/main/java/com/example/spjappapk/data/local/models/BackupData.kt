package com.example.spjappapk.data.local.models

import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.PaymentEntity
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.local.entities.Visit
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val exportTimestamp: Long = System.currentTimeMillis(),
    val appVersionName: String = "",
    val visits: List<Visit> = emptyList(),
    val salesOrders: List<SalesOrder> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val payments: List<PaymentEntity> = emptyList()
)

data class RestoreResult(
    val visitsCount: Int = 0,
    val ordersCount: Int = 0,
    val customersCount: Int = 0,
    val paymentsCount: Int = 0
)
