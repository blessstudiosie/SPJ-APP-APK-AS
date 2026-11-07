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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrders(orders: List<SalesOrder>)

    @Query("SELECT * FROM sales_orders WHERE status = 'PENDING'")
    suspend fun getUnsyncedOrders(): List<SalesOrder>

    @Query("SELECT * FROM sales_orders WHERE remote_id IS NOT NULL AND status = 'PROCESSED'")
    suspend fun getOrdersAwaitingFeedback(): List<SalesOrder>

    @Query("UPDATE sales_orders SET status = :status, remote_id = :remoteId, admin_notes = :notes WHERE id = :id")
    suspend fun updateOrderStatus(id: Long, status: String, remoteId: String?, notes: String? = null)

    @Query("UPDATE sales_orders SET status = :status, admin_notes = :notes WHERE remote_id = :remoteId")
    suspend fun updateFeedbackByRemoteId(remoteId: String, status: String, notes: String?)
}
