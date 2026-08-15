package com.example.spjappapk.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spjappapk.data.local.dao.SalesDetailDao
import com.example.spjappapk.data.local.dao.SalesMasterDao
import com.example.spjappapk.data.local.dao.SalesOrderDao
import com.example.spjappapk.data.local.entities.SalesDetailEntity
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.remote.models.SalesDetail
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import com.example.spjappapk.data.sync.SyncWorker
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class SalesRepository(
    private val salesOrderDao: SalesOrderDao,
    private val salesMasterDao: SalesMasterDao,
    private val salesDetailDao: SalesDetailDao,
    private val salesPersonDao: com.example.spjappapk.data.local.dao.SalesPersonDao,
    private val context: Context
) {
    val allSales: Flow<List<SalesOrder>> = salesOrderDao.getAllOrders()
    val allSalesMaster: Flow<List<SalesMaster>> = salesMasterDao.getAllSalesMaster()

    suspend fun insertOrder(order: SalesOrder) {
        salesOrderDao.insertOrder(order)
        scheduleSync()
    }

    suspend fun refreshSalesPersons(isFullSync: Boolean = false) {
        try {
            val lastUpdate = if (isFullSync) null else salesPersonDao.getMaxUpdatedAt()
            val response = SupabaseClient.client.postgrest["sales_persons"].select {
                if (isFullSync) {
                    limit(5000)
                } else if (lastUpdate != null) {
                    filter { gt("updated_at", lastUpdate) }
                }
            }
            val persons = Json { ignoreUnknownKeys = true }.decodeFromString<List<com.example.spjappapk.data.local.entities.SalesPersonEntity>>(response.data)
            salesPersonDao.insertAll(persons)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshSalesMaster(userId: String, isManager: Boolean, isFullSync: Boolean = false) {
        try {
            val allowedStatuses = listOf("SO", "ON PROSES", "DALAM PENGIRIMAN", "TEMPO", "DONE")
            val lastUpdate = if (isFullSync) null else salesMasterDao.getMaxUpdatedAt()
            
            val allData = mutableListOf<SalesMaster>()
            var offset = 0
            val pageSize = 1000

            while (true) {
                val response = SupabaseClient.client.postgrest["sales"]
                    .select {
                        if (!isManager) {
                            filter { eq("sales_person_id", userId) }
                        }
                        filter { isIn("status", allowedStatuses) }
                        if (lastUpdate != null) {
                            filter { gt("updated_at", lastUpdate) }
                        }
                        range(offset.toLong(), (offset + pageSize - 1).toLong())
                    }
                val batch = Json { ignoreUnknownKeys = true }.decodeFromString<List<SalesMaster>>(response.data)
                allData.addAll(batch)
                
                if (batch.size < pageSize) break
                offset += pageSize
            }

            if (allData.isNotEmpty()) {
                salesMasterDao.insertSalesMaster(allData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getMonthlyOmzet(userId: String, start: Long, end: Long): Double {
        return salesMasterDao.getMonthlyOmzet(userId, start, end) ?: 0.0
    }

    suspend fun getTotalMonthlyOmzet(start: Long, end: Long): Double {
        return salesMasterDao.getTotalMonthlyOmzet(start, end) ?: 0.0
    }

    suspend fun getTeamMonthlyBreakdown(start: Long, end: Long): List<com.example.spjappapk.data.local.dao.SalesPersonOmzet> {
        return salesMasterDao.getTeamMonthlyBreakdown(start, end)
    }

    suspend fun getSalesDetails(salesId: String): List<SalesDetail> {
        // Check local cache first
        val cached = salesDetailDao.getDetailsBySaleId(salesId)
        if (cached.isNotEmpty()) {
            return cached.map { it.toRemoteModel() }
        }

        return try {
            val response = SupabaseClient.client.postgrest["sales_details"]
                .select {
                    filter { eq("sale_id", salesId) }
                }
            val remoteDetails = Json { ignoreUnknownKeys = true }.decodeFromString<List<SalesDetail>>(response.data)
            
            // Save to local cache
            if (remoteDetails.isNotEmpty()) {
                salesDetailDao.insertAll(remoteDetails.map { it.toLocalEntity() })
            }
            
            remoteDetails
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun SalesDetail.toLocalEntity(): SalesDetailEntity {
        return SalesDetailEntity(
            id = id,
            saleId = saleId,
            productId = productId,
            quantity = quantity,
            price = price,
            priceCategory = priceCategory,
            subtotal = subtotal,
            itemName = itemName,
            createdAt = createdAt,
            nota = nota,
            isCheckedSystem2 = isCheckedSystem2
        )
    }

    private fun SalesDetailEntity.toRemoteModel(): SalesDetail {
        return SalesDetail(
            id = id,
            saleId = saleId,
            productId = productId,
            quantity = quantity,
            price = price,
            priceCategory = priceCategory,
            subtotal = subtotal,
            itemName = itemName,
            createdAt = createdAt,
            nota = nota,
            isCheckedSystem2 = isCheckedSystem2
        )
    }

    suspend fun clearAndRefreshSalesMaster(userId: String, isManager: Boolean) {
        salesMasterDao.clearAll()
        refreshSalesMaster(userId, isManager, isFullSync = true)
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
