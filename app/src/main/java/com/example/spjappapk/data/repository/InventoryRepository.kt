package com.example.spjappapk.data.repository

import com.example.spjappapk.data.local.dao.InventoryDao
import com.example.spjappapk.data.local.entities.Inventory
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val supabaseClient: io.github.jan.supabase.SupabaseClient
) {
    val allInventory: Flow<List<Inventory>> = inventoryDao.getAllInventory()

    suspend fun refreshInventory() {
        try {
            // Fetch from Supabase table "products" as defined in gemini.md
            val remoteData = supabaseClient.postgrest["products"].select().decodeList<Inventory>()
            if (remoteData.isNotEmpty()) {
                inventoryDao.insertAll(remoteData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
