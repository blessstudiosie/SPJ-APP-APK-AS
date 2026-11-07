package com.example.spjappapk.data.local.dao

import androidx.room.*
import com.example.spjappapk.data.local.entities.PaymentEntity
import com.example.spjappapk.data.local.entities.SalesMaster
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY payment_date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE sale_id = :saleId ORDER BY payment_date DESC")
    fun getPaymentsBySaleId(saleId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE is_synced = 0")
    suspend fun getUnsyncedPayments(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<PaymentEntity>)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Transaction
    suspend fun insertPaymentAndUpdateSale(payment: PaymentEntity, sale: SalesMaster) {
        insertPayment(payment)
        // Note: The caller should have updated the SalesMaster object with new paid/remaining values
        // We use an external DAO or assume this DAO has access to sales_master if we add @Update for it here
        updateSaleInternal(sale)
    }

    @Update(entity = SalesMaster::class)
    suspend fun updateSaleInternal(sale: SalesMaster)
}
