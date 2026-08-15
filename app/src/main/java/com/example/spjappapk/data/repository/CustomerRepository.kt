package com.example.spjappapk.data.repository

import com.example.spjappapk.data.local.dao.CustomerDao
import com.example.spjappapk.data.local.dao.SyncLogDao
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.SyncLog
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val syncLogDao: SyncLogDao,
    private val supabaseClient: io.github.jan.supabase.SupabaseClient
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun refreshCustomers(isFullSync: Boolean = false): Unit = withContext(Dispatchers.IO) {
        try {
            syncLogDao.insert(SyncLog(event = "Customers", status = "INFO", message = if (isFullSync) "Full Refresh customers..." else "Incremental Sync customers..."))
            
            val remoteData: List<Customer>
            val lastUpdate = if (isFullSync) null else customerDao.getMaxUpdatedAt()

            withTimeout(15000L) {
                remoteData = supabaseClient.postgrest["customers"].select {
                    if (isFullSync) {
                        limit(5000)
                    } else if (lastUpdate != null) {
                        filter { gt("updated_at", lastUpdate) }
                    }
                }.decodeList<Customer>()
            }

            if (remoteData.isNotEmpty()) {
                customerDao.insertAll(remoteData)
                syncLogDao.insert(SyncLog(event = "Customers", status = "SUCCESS", message = "Berhasil menarik ${remoteData.size} pelanggan"))
            } else {
                syncLogDao.insert(SyncLog(event = "Customers", status = "SUCCESS", message = "Tidak ada data pelanggan baru"))
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown Error"
            syncLogDao.insert(SyncLog(event = "Customers", status = "ERROR", message = "Gagal akses server: $errorMsg"))
        }
    }

    suspend fun clearAndRefresh(): Unit = withContext(Dispatchers.IO) {
        customerDao.clearAll()
        refreshCustomers(isFullSync = true)
    }

    suspend fun addCustomer(customer: Customer): Unit = withContext(Dispatchers.IO) {
        try {
            // Simpan lokal dulu sebagai PENDING
            customerDao.insertAll(listOf(customer.copy(syncStatus = "PENDING")))
            
            val insertData = mutableMapOf<String, Any?>(
                "id" to customer.remoteId,
                "name" to customer.name,
                "address" to customer.address,
                "owner_name" to (customer.ownerName ?: ""),
                "phone" to customer.phone,
                "latitude" to customer.latitude,
                "longitude" to customer.longitude,
                "sales_person_id" to customer.salesPersonId,
                "sales_person" to customer.salesPersonName
            )

            // Coba push langsung
            supabaseClient.postgrest["customers"].insert(insertData)
            
            // Jika berhasil push, update status jadi SUCCESS
            customerDao.updateSyncStatus(customer.remoteId, "SUCCESS")
            syncLogDao.insert(SyncLog(event = "Customers", status = "SUCCESS", message = "Pelanggan ${customer.name} berhasil didaftarkan"))
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Offline"
            syncLogDao.insert(SyncLog(event = "Customers", status = "INFO", message = "Pelanggan disimpan lokal (Akan sinkron saat online)"))
        }
    }



    suspend fun checkAndFixOrphanCustomer(remoteId: String, name: String, salesId: String?, salesName: String?) = withContext(Dispatchers.IO) {
        try {
            // Cek apakah sudah ada di server
            val response = supabaseClient.postgrest["customers"]
                .select { filter { eq("id", remoteId) } }
            
            if (response.data == "[]") {
                // Jika tidak ada, daftarkan sebagai placeholder agar visit bisa masuk
                val data = mapOf(
                    "id" to remoteId,
                    "name" to name,
                    "address" to "Auto-generated from Orphan Visit",
                    "sales_person_id" to salesId,
                    "sales_person" to salesName
                )
                supabaseClient.postgrest["customers"].insert(data)
                android.util.Log.d("CustomerRepo", "Fixed orphan customer: $name")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
