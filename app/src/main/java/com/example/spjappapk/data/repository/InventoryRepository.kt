package com.example.spjappapk.data.repository

import com.example.spjappapk.data.local.dao.InventoryDao
import com.example.spjappapk.data.local.dao.SyncLogDao
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.data.local.entities.SyncLog
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
            
            val remoteData: List<Inventory>
            val lastUpdate = if (isFullSync) null else inventoryDao.getMaxUpdatedAt()

            withTimeout(60000L) { // Tingkatkan timeout ke 60 detik
                remoteData = supabaseClient.postgrest["products"].select {
                    if (isFullSync) {
                        limit(5000)
                    } else if (lastUpdate != null) {
                        filter { gt("updated_at", lastUpdate) }
                    }
                }.decodeList<Inventory>()
            }

            if (remoteData.isNotEmpty()) {
                inventoryDao.insertAll(remoteData)
                syncLogDao.insert(SyncLog(event = "Inventory", status = "SUCCESS", message = "Berhasil menarik ${remoteData.size} produk"))
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
