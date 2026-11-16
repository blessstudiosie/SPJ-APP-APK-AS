package com.example.spjappapk.data.repository

import com.example.spjappapk.data.local.dao.CustomerDao
import com.example.spjappapk.data.local.dao.SyncLogDao
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.SyncLog
import com.example.spjappapk.util.retryNetworkCall
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val syncLogDao: SyncLogDao,
    private val supabaseClient: io.github.jan.supabase.SupabaseClient
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun refreshCustomers(isFullSync: Boolean = false): Unit = withContext(Dispatchers.IO) {
        try {
            syncLogDao.insert(SyncLog(event = "Customers", status = "INFO", message = if (isFullSync) "Full Refresh customers..." else "Incremental Sync customers..."))
            
            val allRemoteData = mutableListOf<Customer>()
            var offset = 0
            val pageSize = 1000
            val lastUpdate = if (isFullSync) null else customerDao.getMaxUpdatedAt()

            withTimeout(120000L) {
                while (true) {
                    val batch = retryNetworkCall(times = 3) {
                        val response = supabaseClient.postgrest["customers"].select {
                            if (lastUpdate != null) {
                                filter { gt("updated_at", lastUpdate) }
                            }
                            range(offset.toLong(), (offset + pageSize - 1).toLong())
                        }
                        response.decodeList<Customer>()
                    }
                    allRemoteData.addAll(batch)
                    
                    if (batch.size < pageSize) break
                    offset += pageSize
                }
            }

            if (allRemoteData.isNotEmpty()) {
                customerDao.insertAll(allRemoteData)
                syncLogDao.insert(SyncLog(event = "Customers", status = "SUCCESS", message = "Berhasil menarik ${allRemoteData.size} pelanggan"))
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
            
            val insertData = buildJsonObject {
                put("id", customer.remoteId)
                put("name", customer.name.trim())
                put("address", customer.address ?: "")
                put("owner_name", customer.ownerName ?: "")
                put("phone", customer.phone ?: "")
                put("latitude", customer.latitude ?: 0.0)
                put("longitude", customer.longitude ?: 0.0)
                put("jalur_pengiriman", customer.deliveryRoute ?: "LOBAR-MATARAM")
                put("limit_piutang", customer.creditLimit)
                if (!customer.salesPersonId.isNullOrBlank()) {
                    put("sales_person_id", customer.salesPersonId)
                }
                if (!customer.salesPersonName.isNullOrBlank()) {
                    put("sales_person", customer.salesPersonName)
                }
            }

            // Coba push langsung menggunakan upsert
            supabaseClient.postgrest["customers"].upsert(insertData)
            
            // Jika berhasil push, update status jadi SUCCESS
            customerDao.updateSyncStatus(customer.remoteId, "SUCCESS")
            syncLogDao.insert(SyncLog(event = "Customers", status = "SUCCESS", message = "Pelanggan ${customer.name} berhasil didaftarkan"))
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Offline"
            syncLogDao.insert(SyncLog(event = "Customers", status = "INFO", message = "Pelanggan disimpan lokal, akan disinkronkan saat online ($errorMsg)"))
        }
    }

    suspend fun updateCustomerCoordinates(remoteId: String, lat: Double, lng: Double): Unit = withContext(Dispatchers.IO) {
        try {
            // Update local Room database
            customerDao.updateCustomerCoordinates(remoteId, lat, lng)
            
            // Try to push to Supabase
            val updateData = buildJsonObject {
                put("latitude", lat)
                put("longitude", lng)
                put("updated_at", com.example.spjappapk.util.DateTimeUtil.getUtcIsoFormat().format(java.util.Date()))
            }
            supabaseClient.postgrest["customers"].update(updateData) {
                filter { eq("id", remoteId) }
            }
            customerDao.updateSyncStatus(remoteId, "SUCCESS")
            syncLogDao.insert(SyncLog(event = "Customers", status = "SUCCESS", message = "Koordinat outlet berhasil di-lock ke ($lat, $lng)"))
        } catch (e: Exception) {
            syncLogDao.insert(SyncLog(event = "Customers", status = "INFO", message = "Koordinat outlet diperbarui secara lokal (Akan sinkron saat online)"))
        }
    }

    suspend fun checkAndFixOrphanCustomer(remoteId: String, name: String, salesId: String?, salesName: String?) = withContext(Dispatchers.IO) {
        try {
            // Cek apakah sudah ada di server
            val response = supabaseClient.postgrest["customers"]
                .select { filter { eq("id", remoteId) } }
            
            if (response.data == "[]") {
                // Jika tidak ada, daftarkan sebagai placeholder agar visit bisa masuk
                val data = buildJsonObject {
                    put("id", remoteId)
                    put("name", name.trim())
                    put("address", "Auto-generated from Visit")
                    put("jalur_pengiriman", "LOBAR-MATARAM")
                    if (!salesId.isNullOrBlank()) put("sales_person_id", salesId)
                    if (!salesName.isNullOrBlank()) put("sales_person", salesName)
                }
                supabaseClient.postgrest["customers"].upsert(data)
                android.util.Log.d("CustomerRepo", "Fixed orphan customer: $name")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
