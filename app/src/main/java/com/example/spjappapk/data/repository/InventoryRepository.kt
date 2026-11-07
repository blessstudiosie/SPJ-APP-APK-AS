package com.example.spjappapk.data.repository

import com.example.spjappapk.data.local.dao.InventoryDao
import com.example.spjappapk.data.local.dao.SyncLogDao
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.data.local.entities.SyncLog
import com.example.spjappapk.util.retryNetworkCall
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val syncLogDao: SyncLogDao,
    private val supabaseClient: io.github.jan.supabase.SupabaseClient
) {
    val allInventory: Flow<List<Inventory>> = inventoryDao.getAllInventory()

    suspend fun refreshInventory(isFullSync: Boolean = false): Unit = withContext(Dispatchers.IO) {
        try {
            syncLogDao.insert(SyncLog(event = "Inventory", status = "INFO", message = if (isFullSync) "Full Refresh inventory..." else "Incremental Sync inventory..."))
            
            val allRemoteData = mutableListOf<Inventory>()
            var offset = 0
            val pageSize = 1000
            val lastUpdate = if (isFullSync) null else inventoryDao.getMaxUpdatedAt()

            withTimeout(120000L) {
                while (true) {
                    val batch = retryNetworkCall(times = 3) {
                        val response = supabaseClient.postgrest["products"].select {
                            if (lastUpdate != null) {
                                filter { gt("updated_at", lastUpdate) }
                            }
                            range(offset.toLong(), (offset + pageSize - 1).toLong())
                        }
                        response.decodeList<Inventory>()
                    }
                    allRemoteData.addAll(batch)
                    
                    if (batch.size < pageSize) break
                    offset += pageSize
                }
            }

            if (allRemoteData.isNotEmpty()) {
                inventoryDao.insertAll(allRemoteData)
                syncLogDao.insert(SyncLog(event = "Inventory", status = "SUCCESS", message = "Berhasil menarik ${allRemoteData.size} produk"))
            } else {
                syncLogDao.insert(SyncLog(event = "Inventory", status = "SUCCESS", message = "Tidak ada data produk baru"))
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown Error"
            syncLogDao.insert(SyncLog(event = "Inventory", status = "ERROR", message = "Gagal akses server: $errorMsg"))
            throw e
        }
    }

    suspend fun clearAndRefresh(): Unit = withContext(Dispatchers.IO) {
        inventoryDao.clearAll()
        refreshInventory(isFullSync = true)
    }
}
