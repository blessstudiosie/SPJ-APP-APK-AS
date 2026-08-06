package com.example.spjappapk.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spjappapk.data.local.AppDatabase
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.Inventory
import com.example.spjappapk.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val visitDao = database.visitDao()
        val salesOrderDao = database.salesOrderDao()
        val inventoryDao = database.inventoryDao()
        val customerDao = database.customerDao()

        return try {
            // 1. PULL: Refresh Master Data (Inventory & Customers)
            try {
                val remoteProducts = SupabaseClient.client.postgrest["products"].select().decodeList<Inventory>()
                if (remoteProducts.isNotEmpty()) {
                    inventoryDao.insertAll(remoteProducts)
                }

                val remoteCustomers = SupabaseClient.client.postgrest["customers"].select().decodeList<Customer>()
                if (remoteCustomers.isNotEmpty()) {
                    customerDao.insertAll(remoteCustomers)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. PUSH: Sync Visits as Requests (Pesan Kunjungan)
            val unsyncedVisits = visitDao.getUnsyncedVisits()
            unsyncedVisits.forEach { visit ->
                try {
                    var photoUrl: String? = null
                    if (visit.photoPath != null) {
                        val file = File(visit.photoPath)
                        if (file.exists()) {
                            val bucket = SupabaseClient.client.storage["visit_photos"]
                            val fileName = "${visit.userId}_${System.currentTimeMillis()}.jpg"
                            bucket.upload(fileName, file.readBytes())
                            photoUrl = bucket.publicUrl(fileName)
                        }
                    }

                    // Membungkus data kunjungan ke dalam payload untuk antrian
                    val payload = mapOf(
                        "customer_id" to visit.customerId,
                        "location_lat" to visit.locationLat,
                        "location_lng" to visit.locationLng,
                        "photo_url" to photoUrl,
                        "timestamp" to visit.timestamp
                    )

                    // Mengirim sebagai pesan permintaan ke tabel visit_logs_queue
                    SupabaseClient.client.postgrest["visit_logs_queue"].insert(
                        mapOf(
                            "sales_person_id" to visit.userId,
                            "payload" to payload,
                            "status" to "pending"
                        )
                    )
                    visitDao.updateSyncStatus(visit.id, "SYNCED", null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If one visit fails, we continue to others but don't mark this one as synced
                }
            }

            // 3. PUSH: Sync Sales Orders to sales_orders_queue
            val unsyncedOrders = salesOrderDao.getUnsyncedOrders()
            unsyncedOrders.forEach { order ->
                try {
                    val response = SupabaseClient.client.postgrest["sales_orders_queue"].insert(
                        mapOf(
                            "sales_person_id" to order.salesPersonId,
                            "customer_id" to order.customerId,
                            "order_date" to order.orderDate,
                            "notes" to order.notes,
                            "details" to Json.parseToJsonElement(order.details),
                            "status" to "pending"
                        )
                    ) {
                        select() // Get the inserted row back
                    }
                    
                    // Parse response to get the remote UUID
                    val insertedData = Json.parseToJsonElement(response.data).jsonArray
                    if (insertedData.isNotEmpty()) {
                        val remoteId = insertedData[0].jsonObject["id"]?.toString()?.replace("\"", "")
                        salesOrderDao.updateOrderStatus(order.id, "PROCESSED", remoteId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. PULL: Check for Admin Feedback (Status Updates)
            val ordersToCheck = salesOrderDao.getOrdersAwaitingFeedback()
            if (ordersToCheck.isNotEmpty()) {
                val remoteIds = ordersToCheck.map { it.remoteId!! }
                val updates = SupabaseClient.client.postgrest["sales_orders_queue"]
                    .select {
                        filter {
                            isIn("id", remoteIds)
                            neq("status", "pending") // Only get those processed by admin
                        }
                    }
                
                val updateData = Json.parseToJsonElement(updates.data).jsonArray
                updateData.forEach { element ->
                    val obj = element.jsonObject
                    val rId = obj["id"]?.toString()?.replace("\"", "") ?: ""
                    val serverStatus = obj["status"]?.toString()?.replace("\"", "")?.uppercase() ?: ""
                    val adminNotes = obj["error_message"]?.toString()?.replace("\"", "") // Map error_message to notes
                    
                    if (serverStatus == "PROCESSED" || serverStatus == "APPROVED" || serverStatus == "REJECTED" || serverStatus == "PARTIALLY_APPROVED") {
                        salesOrderDao.updateFeedbackByRemoteId(rId, serverStatus, adminNotes)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
