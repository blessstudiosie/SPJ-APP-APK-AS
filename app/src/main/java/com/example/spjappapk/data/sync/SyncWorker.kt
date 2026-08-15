package com.example.spjappapk.data.sync

import kotlinx.coroutines.flow.firstOrNull

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spjappapk.data.local.AppDatabase
import com.example.spjappapk.data.local.entities.SyncLog
import com.example.spjappapk.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as com.example.spjappapk.SPJApplication
        val database = AppDatabase.getDatabase(applicationContext)
        val customerDao = database.customerDao()
        val visitDao = database.visitDao()
        val salesOrderDao = database.salesOrderDao()
        val salesMasterDao = database.salesMasterDao()
        val syncLogDao = database.syncLogDao()

        suspend fun logSync(event: String, status: String, message: String) {
            syncLogDao.insert(SyncLog(event = event, status = status, message = message))
        }

        return try {
            android.util.Log.d("SyncWorker", "doWork() started")
            logSync("General", "INFO", "Starting sync work...")

            // 1. PUSH: Sync New Customers first (To avoid FK errors in visits/orders)
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            if (unsyncedCustomers.isNotEmpty()) {
                logSync("Customers", "INFO", "Syncing ${unsyncedCustomers.size} new customers")
            }
            unsyncedCustomers.forEach { customer ->
                try {
                    val insertData = buildJsonObject {
                        put("id", customer.remoteId)
                        put("name", customer.name)
                        put("address", customer.address ?: "")
                        put("owner_name", customer.ownerName ?: "")
                        put("phone", customer.phone ?: "")
                        put("latitude", customer.latitude ?: 0.0)
                        put("longitude", customer.longitude ?: 0.0)
                        put("sales_person_id", customer.salesPersonId ?: "")
                        put("sales_person", customer.salesPersonName ?: "")
                        put("jalur_pengiriman", customer.deliveryRoute ?: "LOBAR-MATARAM")
                        put("limit_piutang", customer.creditLimit)
                    }
                    SupabaseClient.client.postgrest["customers"].insert(insertData)
                    customerDao.updateSyncStatus(customer.remoteId, "SUCCESS")
                } catch (e: Exception) {
                    logSync("Customers", "ERROR", "Failed to sync customer ${customer.name}: ${e.message}")
                }
            }

            // 2. PULL: Refresh Master Data
            try {
                android.util.Log.d("SyncWorker", "Refreshing master data...")
                app.inventoryRepository.refreshInventory()
                app.customerRepository.refreshCustomers()
                app.salesRepository.refreshSalesPersons()
                
                // Pull sales master, visits and notifications if user is logged in
                val currentUser = app.sessionManager.userFlow.firstOrNull()
                if (currentUser != null) {
                    val isManagerOrOwner = currentUser.role?.lowercase() == "manager" || currentUser.role?.lowercase() == "owner"
                    app.salesRepository.refreshSalesMaster(
                        currentUser.id,
                        isManagerOrOwner
                    )
                    app.visitRepository.refreshVisits(currentUser.id)
                    app.notificationRepository.refreshNotifications(currentUser.id)
                }
                
                android.util.Log.d("SyncWorker", "Master data refresh called")
            } catch (e: Exception) {
                logSync("General", "ERROR", "PULL Master Data failed: ${e.message}")
                e.printStackTrace()
            }

            // 2. PUSH: Sync Visits to visit_logs_queue
            val unsyncedVisits = visitDao.getUnsyncedVisits()
            if (unsyncedVisits.isNotEmpty()) {
                logSync("Visits", "INFO", "Syncing ${unsyncedVisits.size} unsynced visits")
            }
            unsyncedVisits.forEach { visit ->
                try {
                    // Check-in reports go directly to master table check_in_logs
                    val insertData = buildJsonObject {
                        put("sales_person_id", visit.userId)
                        put("sales_person", visit.salesPersonName)
                        put("customer_id", visit.customerId)
                        put("customer_name", visit.customerName)
                        put("latitude", visit.locationLat)
                        put("longitude", visit.locationLng)
                        put("notes", visit.notes ?: "")
                        put("check_in_date", com.example.spjappapk.util.DateTimeUtil.getIsoFormat().format(java.util.Date(visit.timestamp)))
                    }

                    val response = SupabaseClient.client.postgrest["check_in_logs"].insert(insertData) {
                        select()
                    }

                    val insertedData = Json.parseToJsonElement(response.data).jsonArray
                    if (insertedData.isNotEmpty()) {
                        val remoteId = insertedData[0].jsonObject["id"]?.toString()?.replace("\"", "")
                        visitDao.updateSyncStatus(visit.id, "SUCCESS", remoteId)
                        logSync("Visits", "SUCCESS", "Check-in for ${visit.customerName} recorded")
                    }
                } catch (e: Exception) {
                    logSync("Visits", "ERROR", "Failed to sync check-in for ${visit.customerName}: ${e.message}")
                    e.printStackTrace()
                }
            }

            // 3. PUSH: Sync Sales Orders to sales_orders_queue
            val unsyncedOrders = salesOrderDao.getUnsyncedOrders()
            if (unsyncedOrders.isNotEmpty()) {
                logSync("Sales", "INFO", "Syncing ${unsyncedOrders.size} unsynced orders")
            }
            unsyncedOrders.forEach { order ->
                try {
                    val insertData = buildJsonObject {
                        put("requested_by", order.salesPersonName)
                        put("sales_person_id", order.salesPersonId) // Kirim ID Sales
                        put("customer_name", order.customerName)
                        put("customer_id", order.customerId) // Kirim ID Konsumen
                        put("items", Json.parseToJsonElement(order.details))
                        put("notes", order.notes ?: "")
                        put("status", "pending")
                    }

                    val response = SupabaseClient.client.postgrest["sales_orders_queue"].insert(insertData) {
                        select()
                    }

                    val insertedData = Json.parseToJsonElement(response.data).jsonArray
                    if (insertedData.isNotEmpty()) {
                        val remoteId = insertedData[0].jsonObject["id"]?.toString()?.replace("\"", "")
                        salesOrderDao.updateOrderStatus(order.id, "PROCESSED", remoteId)
                        logSync("Sales", "SUCCESS", "Order for ${order.customerName} pushed to queue")
                    }
                } catch (e: Exception) {
                    logSync("Sales", "ERROR", "Failed to push order for ${order.customerName}: ${e.message}")
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
                                
                                // Create local notification record for this change
                                val order = ordersToCheck.find { it.remoteId == rId }
                                val title = if (serverStatus == "APPROVED") "Pesanan Disetujui" else "Pesanan Ditolak"
                                val type = if (serverStatus == "APPROVED") "SUCCESS" else "ERROR"
                                val msg = "Pesanan untuk ${order?.customerName ?: "Pelanggan"} telah di-$serverStatus oleh admin."
                                
                                database.notificationDao().insertAll(listOf(
                                    com.example.spjappapk.data.local.entities.Notification(
                                        id = "notif-$rId-${System.currentTimeMillis()}",
                                        userId = order?.salesPersonId,
                                        title = title,
                                        message = msg + (notes?.let { " Ket: $it" } ?: ""),
                                        type = type,
                                        isRead = false,
                                        createdAt = System.currentTimeMillis().toString()
                                    )
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 5. PULL: Check for Admin Feedback for Visits (Obsolete if direct to check_in_logs, but keeping for legacy queue)
            val visitsToCheck = visitDao.getVisitsAwaitingFeedback()
            if (visitsToCheck.isNotEmpty()) {
                val remoteIds = visitsToCheck.mapNotNull { it.remoteId }
                if (remoteIds.isNotEmpty()) {
                    try {
                        val updates = SupabaseClient.client.postgrest["visit_logs_queue"]
                            .select {
                                filter {
                                    isIn("id", remoteIds)
                                    neq("status", "pending")
                                }
                            }

                        val updateData = Json.parseToJsonElement(updates.data).jsonArray
                        updateData.forEach { element ->
                            val obj = element.jsonObject
                            val rId = obj["id"]?.toString()?.replace("\"", "") ?: ""
                            val serverStatus = obj["status"]?.toString()?.replace("\"", "")?.lowercase() ?: ""

                            if (serverStatus == "approved" || serverStatus == "rejected") {
                                visitDao.updateSyncStatusByRemoteId(rId, serverStatus.uppercase())
                            }
                        }
                    } catch (e: Exception) {
                        // Log specifically if table is missing
                        android.util.Log.e("SyncWorker", "Feedback from visit_logs_queue failed: ${e.message}")
                    }
                }
            }

            logSync("General", "SUCCESS", "Sync work completed successfully")
            Result.success()
        } catch (e: Exception) {
            logSync("General", "ERROR", "Sync work failed: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }
}
