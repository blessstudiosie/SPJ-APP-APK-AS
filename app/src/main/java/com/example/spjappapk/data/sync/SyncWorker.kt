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
import kotlinx.serialization.json.JsonNull
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

        suspend fun ensureCustomerExistsOnServer(
            customerId: String,
            customerName: String,
            salesPersonId: String?,
            salesPersonName: String?,
            fallbackLat: Double? = null,
            fallbackLng: Double? = null
        ): String? {
            return try {
                // 1. Cek apakah ID konsumen sudah ada di tabel customers Supabase
                val byIdResp = SupabaseClient.client.postgrest["customers"].select {
                    filter { eq("id", customerId) }
                }
                val byIdArr = Json.parseToJsonElement(byIdResp.data).jsonArray
                if (byIdArr.isNotEmpty()) {
                    return customerId
                }

                // 2. Cek apakah nama toko sudah ada di Supabase dengan ID yang berbeda
                val cleanName = customerName.trim()
                if (cleanName.isNotEmpty()) {
                    val byNameResp = SupabaseClient.client.postgrest["customers"].select {
                        filter { ilike("name", cleanName) }
                    }
                    val byNameArr = Json.parseToJsonElement(byNameResp.data).jsonArray
                    if (byNameArr.isNotEmpty()) {
                        val existingId = byNameArr[0].jsonObject["id"]?.toString()?.replace("\"", "")
                        if (!existingId.isNullOrBlank()) {
                            return existingId
                        }
                    }
                }

                // 3. Jika belum ada sama sekali di server, daftarkan dari data lokal Room menggunakan buildJsonObject
                val localCust = customerDao.getCustomerById(customerId)
                val validRoute = localCust?.deliveryRoute?.uppercase()?.trim()?.takeIf {
                    it in listOf("LOBAR-MATARAM", "LOTIM", "LOTENG", "KLU")
                } ?: "LOBAR-MATARAM"

                val customerPayload = buildJsonObject {
                    put("id", customerId)
                    put("name", (localCust?.name ?: cleanName).trim().ifEmpty { "Toko $customerId" })
                    put("address", localCust?.address ?: "")
                    put("owner_name", localCust?.ownerName ?: "")
                    put("phone", localCust?.phone ?: "")
                    put("latitude", localCust?.latitude ?: fallbackLat ?: 0.0)
                    put("longitude", localCust?.longitude ?: fallbackLng ?: 0.0)
                    put("jalur_pengiriman", validRoute)
                    put("limit_piutang", localCust?.creditLimit ?: 0.0)
                    val spId = localCust?.salesPersonId ?: salesPersonId
                    val spName = localCust?.salesPersonName ?: salesPersonName
                    if (!spId.isNullOrBlank()) put("sales_person_id", spId)
                    if (!spName.isNullOrBlank()) put("sales_person", spName)
                }

                SupabaseClient.client.postgrest["customers"].upsert(customerPayload)
                customerDao.updateSyncStatus(customerId, "SUCCESS")
                logSync("Customers", "SUCCESS", "Auto-synced customer ${(localCust?.name ?: cleanName)} to server")
                customerId
            } catch (e: Exception) {
                logSync("Customers", "WARN", "Could not auto-verify customer $customerName: ${e.message}")
                null
            }
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
                    val validRoute = customer.deliveryRoute?.uppercase()?.trim()?.takeIf {
                        it in listOf("LOBAR-MATARAM", "LOTIM", "LOTENG", "KLU")
                    } ?: "LOBAR-MATARAM"

                    val insertData = buildJsonObject {
                        put("id", customer.remoteId)
                        put("name", customer.name.trim())
                        put("address", customer.address ?: "")
                        put("owner_name", customer.ownerName ?: "")
                        put("phone", customer.phone ?: "")
                        put("latitude", customer.latitude ?: 0.0)
                        put("longitude", customer.longitude ?: 0.0)
                        put("jalur_pengiriman", validRoute)
                        put("limit_piutang", customer.creditLimit)
                        if (!customer.salesPersonId.isNullOrBlank()) {
                            put("sales_person_id", customer.salesPersonId)
                        }
                        if (!customer.salesPersonName.isNullOrBlank()) {
                            put("sales_person", customer.salesPersonName)
                        }
                    }

                    SupabaseClient.client.postgrest["customers"].upsert(insertData)
                    customerDao.updateSyncStatus(customer.remoteId, "SUCCESS")
                    logSync("Customers", "SUCCESS", "Customer ${customer.name} synced to server")
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("23505") || msg.contains("already exists")) {
                        customerDao.updateSyncStatus(customer.remoteId, "SUCCESS")
                        logSync("Customers", "SUCCESS", "Customer ${customer.name} already exists on server")
                    } else {
                        logSync("Customers", "ERROR", "Failed to sync customer ${customer.name}: $msg")
                    }
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
                    app.paymentRepository.refreshPayments()
                }
                
                android.util.Log.d("SyncWorker", "Master data refresh called")
            } catch (e: Exception) {
                logSync("General", "ERROR", "PULL Master Data failed: ${e.message}")
                e.printStackTrace()
            }

            // 2. PUSH: Sync Visits to check_in_logs
            val unsyncedVisits = visitDao.getUnsyncedVisits()
            if (unsyncedVisits.isNotEmpty()) {
                logSync("Visits", "INFO", "Syncing ${unsyncedVisits.size} unsynced visits")
            }
            unsyncedVisits.forEach { visit ->
                try {
                    val checkInDateIso = com.example.spjappapk.util.DateTimeUtil.getUtcIsoFormat().format(java.util.Date(visit.timestamp))

                    // Pastikan customer_id sudah terdaftar di tabel customers Supabase (mencegah error FK 23503)
                    val resolvedCustomerId = ensureCustomerExistsOnServer(
                        customerId = visit.customerId,
                        customerName = visit.customerName,
                        salesPersonId = visit.userId,
                        salesPersonName = visit.salesPersonName,
                        fallbackLat = visit.locationLat,
                        fallbackLng = visit.locationLng
                    )

                    val insertData = buildJsonObject {
                        put("sales_person_id", visit.userId)
                        put("sales_person", visit.salesPersonName)
                        if (!resolvedCustomerId.isNullOrBlank()) {
                            put("customer_id", resolvedCustomerId)
                        } else {
                            put("customer_id", JsonNull)
                        }
                        put("customer_name", visit.customerName)
                        put("latitude", visit.locationLat)
                        put("longitude", visit.locationLng)
                        put("notes", visit.notes ?: "")
                        put("check_in_date", checkInDateIso)
                    }

                    val response = try {
                        SupabaseClient.client.postgrest["check_in_logs"].insert(insertData) {
                            select()
                        }
                    } catch (fkEx: Exception) {
                        val errMsg = fkEx.message ?: ""
                        if (errMsg.contains("23503") || errMsg.contains("foreign key")) {
                            val fallbackData = buildJsonObject {
                                put("sales_person_id", visit.userId)
                                put("sales_person", visit.salesPersonName)
                                put("customer_id", JsonNull)
                                put("customer_name", visit.customerName)
                                put("latitude", visit.locationLat)
                                put("longitude", visit.locationLng)
                                put("notes", visit.notes ?: "")
                                put("check_in_date", checkInDateIso)
                            }
                            SupabaseClient.client.postgrest["check_in_logs"].insert(fallbackData) {
                                select()
                            }
                        } else {
                            throw fkEx
                        }
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

            // 3. PUSH: Sync Sales Orders to sales_orders_queue & sales (status = SO)
            val unsyncedOrders = salesOrderDao.getUnsyncedOrders()
            if (unsyncedOrders.isNotEmpty()) {
                logSync("Sales", "INFO", "Syncing ${unsyncedOrders.size} unsynced orders (SO)")
            }
            unsyncedOrders.forEach { order ->
                try {
                    val saleId = order.createdAt?.takeIf { it.length >= 30 } ?: java.util.UUID.randomUUID().toString()
                    val datePrefix = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US).format(java.util.Date(order.timestamp))
                    val rawNotes = order.notes ?: ""
                    val notaNum = rawNotes.substringBefore(" | ").trim().takeIf { it.startsWith("SO-", ignoreCase = true) }
                        ?: "SO-$datePrefix-${saleId.take(6).uppercase(java.util.Locale.US)}"
                    val extraNote = if (rawNotes.contains(" | ")) rawNotes.substringAfter(" | ").trim() else if (!rawNotes.startsWith("SO-", ignoreCase = true)) rawNotes.trim() else ""

                    val parsedItems = try {
                        Json { ignoreUnknownKeys = true }.decodeFromString<List<com.example.spjappapk.data.local.entities.SalesOrderItem>>(order.details)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val totalAmount = parsedItems.sumOf { it.quantity * it.displayPrice }.takeIf { it > 0.0 } ?: order.totalAmount
                    val enrichedItemsJson = kotlinx.serialization.json.buildJsonArray {
                        parsedItems.forEach { item ->
                            add(buildJsonObject {
                                put("product_id", item.productId)
                                put("item_name", item.displayProductName)
                                put("product_name", item.displayProductName)
                                put("qty", item.quantity.toInt())
                                put("price", item.displayPrice)
                                put("unit_price", item.displayPrice)
                                put("price_category", item.priceCategory.ifBlank { "R" })
                                put("subtotal", item.quantity * item.displayPrice)
                            })
                        }
                    }

                    val resolvedOrderCustomerId = ensureCustomerExistsOnServer(
                        customerId = order.customerId,
                        customerName = order.customerName,
                        salesPersonId = order.salesPersonId,
                        salesPersonName = order.salesPersonName
                    ) ?: order.customerId

                    val insertData = buildJsonObject {
                        put("id", saleId)
                        put("requested_by", order.salesPersonName)
                        put("sales_person_id", order.salesPersonId)
                        put("customer_name", order.customerName)
                        put("customer_id", resolvedOrderCustomerId)
                        put("items", if (parsedItems.isNotEmpty()) enrichedItemsJson else Json.parseToJsonElement(order.details))
                        put("notes", if (extraNote.isNotBlank()) "$notaNum | $extraNote" else notaNum)
                        put("status", "pending")
                    }

                    var finalRemoteId = saleId
                    try {
                        val response = SupabaseClient.client.postgrest["sales_orders_queue"].insert(insertData) {
                            select()
                        }
                        val insertedData = Json.parseToJsonElement(response.data).jsonArray
                        if (insertedData.isNotEmpty()) {
                            finalRemoteId = insertedData[0].jsonObject["id"]?.toString()?.replace("\"", "") ?: saleId
                        }
                    } catch (queueEx: Exception) {
                        val msg = queueEx.message ?: ""
                        if (!msg.contains("23505") && !msg.contains("duplicate key")) {
                            throw queueEx
                        }
                    }

                    // Langsung daftarkan juga ke tabel utama 'sales' & 'sales_details' dengan status "SO"
                    try {
                        val nowIso = com.example.spjappapk.util.DateTimeUtil.getUtcIsoFormat().format(java.util.Date(order.timestamp))
                        val descText = if (extraNote.isNotBlank()) {
                            "[NEW] Pesanan SO dari Mobile Sales (${order.salesPersonName}) - $extraNote"
                        } else {
                            "[NEW] Pesanan SO dari Mobile Sales (${order.salesPersonName})"
                        }
                        val saleMasterJson = buildJsonObject {
                            put("id", finalRemoteId)
                            put("nota", notaNum)
                            put("customer_id", resolvedOrderCustomerId)
                            put("customer_name", order.customerName)
                            put("sales_person_id", order.salesPersonId)
                            put("sales_person", order.salesPersonName)
                            put("order_date", nowIso)
                            put("status", "SO")
                            put("total", totalAmount)
                            put("paid", 0.0)
                            put("remaining", totalAmount)
                            put("description", descText)
                            put("is_checked_system2", false)
                            put("is_deleted", false)
                        }
                        SupabaseClient.client.postgrest["sales"].upsert(saleMasterJson)

                        if (parsedItems.isNotEmpty()) {
                            val detailsArray = kotlinx.serialization.json.buildJsonArray {
                                parsedItems.forEach { item ->
                                    add(buildJsonObject {
                                        put("id", java.util.UUID.randomUUID().toString())
                                        put("sale_id", finalRemoteId)
                                        put("nota", notaNum)
                                        put("product_id", item.productId)
                                        put("item_name", item.displayProductName)
                                        put("qty", item.quantity.toInt())
                                        put("price", item.displayPrice)
                                        put("price_category", item.priceCategory.ifBlank { "R" })
                                        put("subtotal", item.quantity * item.displayPrice)
                                        put("is_checked_system2", false)
                                        put("is_deleted", false)
                                    })
                                }
                            }
                            SupabaseClient.client.postgrest["sales_details"].insert(detailsArray)
                        }
                    } catch (directSaleEx: Exception) {
                        logSync("Sales", "WARN", "Direct SO sync warning for ${order.customerName}: ${directSaleEx.message}")
                    }

                    salesOrderDao.updateOrderStatus(order.id, "SO", finalRemoteId)
                    logSync("Sales", "SUCCESS", "Order SO ($notaNum) for ${order.customerName} synced")
                } catch (e: Exception) {
                    logSync("Sales", "ERROR", "Failed to push order for ${order.customerName}: ${e.message}")
                    e.printStackTrace()
                }
            }

            // 4. PUSH: Sync Payments to Supabase
            val unsyncedPayments = database.paymentDao().getUnsyncedPayments()
            if (unsyncedPayments.isNotEmpty()) {
                logSync("Payments", "INFO", "Syncing ${unsyncedPayments.size} unsynced payments")
            }
            unsyncedPayments.forEach { payment ->
                try {
                    val insertData = buildJsonObject {
                        put("id", payment.id)
                        put("sale_id", payment.saleId)
                        put("payment_date", com.example.spjappapk.util.DateTimeUtil.getUtcIsoFormat().format(java.util.Date(payment.paymentDate)))
                        put("amount", payment.amount)
                        put("payment_method", payment.paymentMethod)
                        put("status", payment.status)
                        put("notes", payment.notes ?: "")
                    }

                    try {
                        SupabaseClient.client.postgrest["payments"].insert(insertData)
                    } catch (insertEx: Exception) {
                        val msg = insertEx.message ?: ""
                        if (msg.contains("23505") || msg.contains("duplicate key")) {
                            try {
                                SupabaseClient.client.postgrest["payments"].update(insertData) {
                                    filter { eq("id", payment.id) }
                                }
                            } catch (updateEx: Exception) {
                                SupabaseClient.client.postgrest["payments"].delete {
                                    filter { eq("id", payment.id) }
                                }
                                SupabaseClient.client.postgrest["payments"].insert(insertData)
                            }
                        } else {
                            throw insertEx
                        }
                    }
                    database.paymentDao().markAsSynced(payment.id)
                    logSync("Payments", "SUCCESS", "Payment for sale ${payment.saleId} synced")
                } catch (e: Exception) {
                    logSync("Payments", "ERROR", "Failed to sync payment: ${e.message}")
                }
            }

            // 5. PUSH: Sync Updated Sales (Payments results) to Supabase
            val unsyncedSales = salesMasterDao.getUnsyncedSales()
            unsyncedSales.forEach { sale ->
                try {
                    val updateData = buildJsonObject {
                        put("paid", sale.paid)
                        put("remaining", sale.remaining)
                        put("status", sale.status)
                        put("updated_at", com.example.spjappapk.util.DateTimeUtil.getUtcIsoFormat().format(java.util.Date()))
                    }
                    SupabaseClient.client.postgrest["sales"].update(updateData) {
                        filter { eq("id", sale.id) }
                    }
                    salesMasterDao.markAsSynced(sale.id)
                    logSync("Sales", "SUCCESS", "Sale ${sale.nota} balance updated on server")
                } catch (e: Exception) {
                    logSync("Sales", "ERROR", "Failed to sync sale update: ${e.message}")
                }
            }

            // 6. PULL: Check for Admin Feedback for Sales Orders
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
