package com.example.spjappapk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spjappapk.data.local.entities.Inventory
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory")
    fun getAllInventory(): Flow<List<Inventory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inventory: List<Inventory>)
}
