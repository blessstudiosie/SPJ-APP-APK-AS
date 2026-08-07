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
        android.util.Log.d("InventoryRepo", "Refreshing inventory from 'products' table...")
        // Fetch from Supabase table "products" as defined in gemini.md
        val remoteData = supabaseClient.postgrest["products"].select().decodeList<Inventory>()
        android.util.Log.d("InventoryRepo", "Fetched ${remoteData.size} items")
        if (remoteData.isNotEmpty()) {
            inventoryDao.insertAll(remoteData)
            android.util.Log.d("InventoryRepo", "Inserted items into local DB")
        }
    }
}
