package com.example.spjappapk.data.local.dao

import androidx.room.*
import com.example.spjappapk.data.local.entities.SalesMaster
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesMasterDao {
    @Query("SELECT * FROM sales_master ORDER BY order_date DESC")
    fun getAllSalesMaster(): Flow<List<SalesMaster>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesMaster(sales: List<SalesMaster>)

    @Query("DELETE FROM sales_master")
    suspend fun clearAll()

    @Query("SELECT SUM(total_amount) FROM sales_master WHERE (is_deleted = 0 OR is_deleted IS NULL) AND sales_person_id = :userId AND (UPPER(TRIM(status)) = 'TEMPO' OR UPPER(TRIM(status)) = 'DONE') AND (CASE WHEN delivery_date IS NOT NULL THEN delivery_date ELSE order_date END) BETWEEN :start AND :end")
    suspend fun getMonthlyOmzet(userId: String, start: Long, end: Long): Double?

    @Query("SELECT SUM(total_amount) FROM sales_master WHERE (is_deleted = 0 OR is_deleted IS NULL) AND (UPPER(TRIM(status)) = 'TEMPO' OR UPPER(TRIM(status)) = 'DONE') AND (CASE WHEN delivery_date IS NOT NULL THEN delivery_date ELSE order_date END) BETWEEN :start AND :end")
    suspend fun getTotalMonthlyOmzet(start: Long, end: Long): Double?

    @Query("SELECT sales_person_name as salesName, SUM(total_amount) as totalAmount FROM sales_master WHERE (is_deleted = 0 OR is_deleted IS NULL) AND (UPPER(TRIM(status)) = 'TEMPO' OR UPPER(TRIM(status)) = 'DONE') AND (CASE WHEN delivery_date IS NOT NULL THEN delivery_date ELSE order_date END) BETWEEN :start AND :end GROUP BY sales_person_name ORDER BY totalAmount DESC")
    suspend fun getTeamMonthlyBreakdown(start: Long, end: Long): List<SalesPersonOmzet>

    @Query("SELECT MAX(updated_at) FROM sales_master")
    suspend fun getMaxUpdatedAt(): String?
}

data class SalesPersonOmzet(
    val salesName: String,
    val totalAmount: Double
)
