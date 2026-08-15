package com.example.spjappapk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.spjappapk.data.local.dao.CustomerDao
import com.example.spjappapk.data.local.dao.InventoryDao
import com.example.spjappapk.data.local.dao.NotificationDao
import com.example.spjappapk.data.local.dao.SalesDetailDao
import com.example.spjappapk.data.local.dao.SalesMasterDao
import com.example.spjappapk.data.local.dao.SalesPersonDao
import com.example.spjappapk.data.local.dao.SalesOrderDao
import com.example.spjappapk.data.local.dao.SyncLogDao
import com.example.spjappapk.data.local.dao.VisitDao
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.data.local.entities.Notification
import com.example.spjappapk.data.local.entities.SalesDetailEntity
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.data.local.entities.SalesPersonEntity
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.local.entities.SyncLog
import com.example.spjappapk.data.local.entities.Visit

@Database(entities = [Visit::class, Inventory::class, SalesOrder::class, SalesMaster::class, Customer::class, SyncLog::class, Notification::class, SalesPersonEntity::class, SalesDetailEntity::class], version = 14, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun visitDao(): VisitDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun salesOrderDao(): SalesOrderDao
    abstract fun salesMasterDao(): SalesMasterDao
    abstract fun salesDetailDao(): SalesDetailDao
    abstract fun salesPersonDao(): SalesPersonDao
    abstract fun notificationDao(): NotificationDao
    abstract fun customerDao(): CustomerDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spj_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
