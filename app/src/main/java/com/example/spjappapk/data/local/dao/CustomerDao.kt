package com.example.spjappapk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spjappapk.data.local.entities.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)

    @Query("UPDATE customers SET latitude = :lat, longitude = :lng, sync_status = 'PENDING' WHERE remote_id = :remoteId")
    suspend fun updateCustomerCoordinates(remoteId: String, lat: Double, lng: Double)

    @Query("SELECT * FROM customers WHERE sync_status = 'PENDING'")
    suspend fun getUnsyncedCustomers(): List<Customer>

    @Query("UPDATE customers SET sync_status = :status WHERE remote_id = :remoteId")
    suspend fun updateSyncStatus(remoteId: String, status: String)

    @Query("DELETE FROM customers")
    suspend fun clearAll()

    @Query("SELECT MAX(updated_at) FROM customers")
    suspend fun getMaxUpdatedAt(): String?
}
