package com.example.spjappapk.data.repository

import android.content.Context
import android.net.Uri
import com.example.spjappapk.data.local.AppDatabase
import com.example.spjappapk.data.local.models.BackupData
import com.example.spjappapk.data.local.models.RestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val visits = database.visitDao().getAllVisits().first()
            val orders = database.salesOrderDao().getAllOrders().first()
            val customers = database.customerDao().getAllCustomers().first()
            val payments = database.paymentDao().getUnsyncedPayments()

            val backup = BackupData(
                exportTimestamp = System.currentTimeMillis(),
                appVersionName = com.example.spjappapk.BuildConfig.VERSION_NAME,
                visits = visits,
                salesOrders = orders,
                customers = customers,
                payments = payments
            )

            val jsonString = json.encodeToString(backup)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                }
            } ?: return@withContext Result.failure(Exception("Gagal membuka file tujuan untuk menulis backup."))

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(uri: Uri): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(Exception("Gagal membaca file backup."))

            val backup = json.decodeFromString<BackupData>(jsonString)

            var restoredVisits = 0
            var restoredOrders = 0
            var restoredCustomers = 0
            var restoredPayments = 0

            if (backup.visits.isNotEmpty()) {
                database.visitDao().insertAll(backup.visits)
                restoredVisits = backup.visits.size
            }

            if (backup.salesOrders.isNotEmpty()) {
                database.salesOrderDao().insertAllOrders(backup.salesOrders)
                restoredOrders = backup.salesOrders.size
            }

            if (backup.customers.isNotEmpty()) {
                database.customerDao().insertAll(backup.customers)
                restoredCustomers = backup.customers.size
            }

            if (backup.payments.isNotEmpty()) {
                database.paymentDao().insertAllPayments(backup.payments)
                restoredPayments = backup.payments.size
            }

            Result.success(
                RestoreResult(
                    visitsCount = restoredVisits,
                    ordersCount = restoredOrders,
                    customersCount = restoredCustomers,
                    paymentsCount = restoredPayments
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
