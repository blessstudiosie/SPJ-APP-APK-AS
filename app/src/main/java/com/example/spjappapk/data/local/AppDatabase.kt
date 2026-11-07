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
import com.example.spjappapk.data.local.entities.PaymentEntity
import com.example.spjappapk.data.local.entities.SalesDetailEntity
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.data.local.entities.SalesPersonEntity
import com.example.spjappapk.data.local.entities.SalesOrder
import com.example.spjappapk.data.local.entities.SyncLog
import com.example.spjappapk.data.local.entities.Visit

@Database(entities = [Visit::class, Inventory::class, SalesOrder::class, SalesMaster::class, Customer::class, SyncLog::class, Notification::class, SalesPersonEntity::class, SalesDetailEntity::class, PaymentEntity::class], version = 17, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun visitDao(): com.example.spjappapk.data.local.dao.VisitDao
    abstract fun inventoryDao(): com.example.spjappapk.data.local.dao.InventoryDao
    abstract fun salesOrderDao(): com.example.spjappapk.data.local.dao.SalesOrderDao
    abstract fun salesMasterDao(): com.example.spjappapk.data.local.dao.SalesMasterDao
    abstract fun salesDetailDao(): com.example.spjappapk.data.local.dao.SalesDetailDao
    abstract fun salesPersonDao(): com.example.spjappapk.data.local.dao.SalesPersonDao
    abstract fun notificationDao(): com.example.spjappapk.data.local.dao.NotificationDao
    abstract fun customerDao(): com.example.spjappapk.data.local.dao.CustomerDao
    abstract fun syncLogDao(): com.example.spjappapk.data.local.dao.SyncLogDao
    abstract fun paymentDao(): com.example.spjappapk.data.local.dao.PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                fun safeExec(sql: String) {
                    try { db.execSQL(sql) } catch (_: Exception) {}
                }
                safeExec("ALTER TABLE payments ADD COLUMN confirmed_at INTEGER")
                safeExec("ALTER TABLE payments ADD COLUMN updated_at TEXT")
                safeExec("ALTER TABLE sales_details_cache ADD COLUMN updated_at TEXT")
                safeExec("ALTER TABLE sales_details_cache ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
                safeExec("ALTER TABLE customers ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
                safeExec("ALTER TABLE inventory ADD COLUMN price_updated_at TEXT")
                safeExec("ALTER TABLE inventory ADD COLUMN created_at TEXT")
                safeExec("ALTER TABLE inventory ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
                safeExec("ALTER TABLE sales_persons_profile ADD COLUMN employment_status TEXT DEFAULT 'AKTIF'")
                safeExec("ALTER TABLE sales_persons_profile ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spj_database"
                )
                .addMigrations(MIGRATION_16_17)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
