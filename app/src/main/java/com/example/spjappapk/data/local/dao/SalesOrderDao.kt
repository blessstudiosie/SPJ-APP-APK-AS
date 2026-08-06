package com.example.spjappapk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spjappapk.data.local.entities.SalesOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesOrderDao {
    @Query("SELECT * FROM sales_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<SalesOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SalesOrder): Long

    @Query("SELECT * FROM sales_orders WHERE status = 'PENDING'")
    suspend fun getUnsyncedOrders(): List<SalesOrder>

    @Query("UPDATE sales_orders SET status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String)
}
