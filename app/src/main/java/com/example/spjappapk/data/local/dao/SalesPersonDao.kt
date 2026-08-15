package com.example.spjappapk.data.local.dao

import androidx.room.*
import com.example.spjappapk.data.local.entities.SalesPersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesPersonDao {
    @Query("SELECT * FROM sales_persons_profile")
    fun getAllSalesPersons(): Flow<List<SalesPersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<SalesPersonEntity>)

    @Query("DELETE FROM sales_persons_profile")
    suspend fun clearAll()

    @Query("SELECT MAX(updated_at) FROM sales_persons_profile")
    suspend fun getMaxUpdatedAt(): String?
}
