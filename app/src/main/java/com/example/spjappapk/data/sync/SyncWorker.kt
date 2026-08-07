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
            android.util.Log.d("SyncWorker", "Starting sync work...")
            // 1. PULL: Refresh Master Data (Inventory & Customers)
            try {
                android.util.Log.d("SyncWorker", "Pulling products...")
                val remoteProducts = SupabaseClient.client.postgrest["products"].select().decodeList<Inventory>()
                android.util.Log.d("SyncWorker", "Received ${remoteProducts.size} products")
                if (remoteProducts.isNotEmpty()) {
                    inventoryDao.insertAll(remoteProducts)
                }

                android.util.Log.d("SyncWorker", "Pulling customers...")
                val remoteCustomers = SupabaseClient.client.postgrest["customers"].select().decodeList<Customer>()
                android.util.Log.d("SyncWorker", "Received ${remoteCustomers.size} customers")
                if (remoteCustomers.isNotEmpty()) {
                    customerDao.insertAll(remoteCustomers)
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncWorker", "Error pulling master data", e)
                e.printStackTrace()
            }

            // 2. PUSH: Sync Visits to visit_logs_queue
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

                    val insertData = mapOf(
                        "sales_person_id" to visit.userId,
                        "sales_person_name" to visit.salesPersonName,
                        "customer_id" to visit.customerId.ifBlank { null },
                        "customer_name" to visit.customerName,
                        "is_new_customer" to visit.isNewCustomer,
                        "latitude" to visit.locationLat,
                        "longitude" to visit.locationLng,
                        "photo_url" to photoUrl,
                        "notes" to visit.notes,
                        "status" to "PENDING"
                    )

                    val response = SupabaseClient.client.postgrest["visit_logs_queue"].insert(insertData) {
                        select()
                    }

                    val insertedData = Json.parseToJsonElement(response.data).jsonArray
                    if (insertedData.isNotEmpty()) {
                        val remoteId = insertedData[0].jsonObject["id"]?.toString()?.replace("\"", "")
                        visitDao.updateSyncStatus(visit.id, "PROCESSED", remoteId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. PUSH: Sync Sales Orders to sales_orders_queue
            val unsyncedOrders = salesOrderDao.getUnsyncedOrders()
            unsyncedOrders.forEach { order ->
                try {
                    val insertData = mapOf(
                        "sales_person_id" to order.salesPersonId,
                        "customer_id" to order.customerId.ifBlank { null },
                        "customer_name" to order.customerName,
                        "items_json" to order.details,
                        "total_amount" to order.totalAmount,
                        "notes" to order.notes,
                        "status" to "PENDING"
                    )

                    val response = SupabaseClient.client.postgrest["sales_orders_queue"].insert(insertData) {
                        select()
                    }

                    val insertedData = Json.parseToJsonElement(response.data).jsonArray
                    if (insertedData.isNotEmpty()) {
                        val remoteId = insertedData[0].jsonObject["id"]?.toString()?.replace("\"", "")
                        salesOrderDao.updateOrderStatus(order.id, "PROCESSED", remoteId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. PULL: Check for Admin Feedback for Sales Orders
            val ordersToCheck = salesOrderDao.getOrdersAwaitingFeedback()
            if (ordersToCheck.isNotEmpty()) {
                val remoteIds = ordersToCheck.mapNotNull { it.remoteId }
                if (remoteIds.isNotEmpty()) {
                    try {
                        val updates = SupabaseClient.client.postgrest["sales_orders_queue"]
                            .select {
                                filter {
                                    isIn("id", remoteIds)
                                    neq("status", "PENDING")
                                }
                            }

                        val updateData = Json.parseToJsonElement(updates.data).jsonArray
                        updateData.forEach { element ->
                            val obj = element.jsonObject
                            val rId = obj["id"]?.toString()?.replace("\"", "") ?: ""
                            val serverStatus = obj["status"]?.toString()?.replace("\"", "")?.uppercase() ?: ""
                            val notes = obj["notes"]?.toString()?.replace("\"", "")

                            if (serverStatus == "APPROVED" || serverStatus == "REJECTED") {
                                salesOrderDao.updateFeedbackByRemoteId(rId, serverStatus, notes)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 5. PULL: Check for Admin Feedback for Visits
            val visitsToCheck = visitDao.getVisitsAwaitingFeedback()
            if (visitsToCheck.isNotEmpty()) {
                val remoteIds = visitsToCheck.mapNotNull { it.remoteId }
                if (remoteIds.isNotEmpty()) {
                    try {
                        val updates = SupabaseClient.client.postgrest["visit_logs_queue"]
                            .select {
                                filter {
                                    isIn("id", remoteIds)
                                    neq("status", "PENDING")
                                }
                            }

                        val updateData = Json.parseToJsonElement(updates.data).jsonArray
                        updateData.forEach { element ->
                            val obj = element.jsonObject
                            val rId = obj["id"]?.toString()?.replace("\"", "") ?: ""
                            val serverStatus = obj["status"]?.toString()?.replace("\"", "")?.uppercase() ?: ""

                            if (serverStatus == "APPROVED" || serverStatus == "REJECTED") {
                                visitDao.updateSyncStatusByRemoteId(rId, serverStatus)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
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
