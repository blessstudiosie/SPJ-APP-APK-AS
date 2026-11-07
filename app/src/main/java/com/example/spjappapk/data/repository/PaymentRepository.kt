package com.example.spjappapk.data.repository

import com.example.spjappapk.data.local.dao.PaymentDao
import com.example.spjappapk.data.local.dao.SalesMasterDao
import com.example.spjappapk.data.local.entities.PaymentEntity
import com.example.spjappapk.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val salesMasterDao: SalesMasterDao
) {
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    fun getPaymentsForSale(saleId: String): Flow<List<PaymentEntity>> = paymentDao.getPaymentsBySaleId(saleId)

    suspend fun getSaleById(saleId: String) = salesMasterDao.getSaleById(saleId)

    suspend fun refreshPayments() = withContext(Dispatchers.IO) {
        try {
            val allData = mutableListOf<PaymentEntity>()
            var offset = 0
            val pageSize = 1000

            while (true) {
                val response = SupabaseClient.client.postgrest["payments"]
                    .select {
                        range(offset.toLong(), (offset + pageSize - 1).toLong())
                    }
                val batch = Json { ignoreUnknownKeys = true }.decodeFromString<List<PaymentEntity>>(response.data)
                val syncedBatch = batch.map { it.copy(isSynced = true) }
                allData.addAll(syncedBatch)

                if (batch.size < pageSize) break
                offset += pageSize
            }

            if (allData.isNotEmpty()) {
                paymentDao.insertAllPayments(allData)

                // Update SalesMaster paid/remaining balance based on APPROVED payments
                val saleIds = allData.map { it.saleId }.distinct()
                for (saleId in saleIds) {
                    val sale = salesMasterDao.getSaleById(saleId) ?: continue
                    val approvedPayments = allData.filter { it.saleId == saleId && it.status.uppercase() == "APPROVED" }
                    val totalApprovedPaid = approvedPayments.sumOf { it.amount }
                    val newRemaining = (sale.totalAmount - totalApprovedPaid).coerceAtLeast(0.0)
                    val newStatus = if (newRemaining <= 0) "DONE" else "TEMPO"

                    if (sale.paid != totalApprovedPaid || sale.remaining != newRemaining) {
                        paymentDao.updateSaleInternal(
                            sale.copy(
                                paid = totalApprovedPaid,
                                remaining = newRemaining,
                                status = newStatus
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addPayment(payment: PaymentEntity) = withContext(Dispatchers.IO) {
        val newPayment = payment.copy(
            status = "PENDING",
            isSynced = false
        )
        paymentDao.insertPayment(newPayment)
    }

    suspend fun syncUnsyncedPayments() = withContext(Dispatchers.IO) {
        val unsynced = paymentDao.getUnsyncedPayments()
        for (payment in unsynced) {
            try {
                SupabaseClient.client.postgrest["payments"].insert(payment)
                paymentDao.markAsSynced(payment.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
