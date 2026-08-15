package com.example.spjappapk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spjappapk.data.local.entities.SalesDetailEntity

@Dao
interface SalesDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<SalesDetailEntity>)

    @Query("SELECT * FROM sales_details_cache WHERE sale_id = :saleId")
    suspend fun getDetailsBySaleId(saleId: String): List<SalesDetailEntity>

    @Query("DELETE FROM sales_details_cache")
    suspend fun clearAll()
}
