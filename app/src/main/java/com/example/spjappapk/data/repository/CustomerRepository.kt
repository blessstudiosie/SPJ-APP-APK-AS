package com.example.spjappapk.data.repository

import com.example.spjappapk.data.local.dao.CustomerDao
import com.example.spjappapk.data.local.entities.Customer
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val supabaseClient: io.github.jan.supabase.SupabaseClient
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun refreshCustomers() {
        android.util.Log.d("CustomerRepo", "Refreshing customers...")
        // Fetch from Supabase table "customers"
        val remoteData = supabaseClient.postgrest["customers"].select().decodeList<Customer>()
        android.util.Log.d("CustomerRepo", "Fetched ${remoteData.size} customers")
        if (remoteData.isNotEmpty()) {
            customerDao.insertAll(remoteData)
        }
    }

    suspend fun addCustomer(customer: Customer) {
        try {
            // Insert locally first
            customerDao.insertAll(listOf(customer))
            
            // Push to queue for desktop review
            supabaseClient.postgrest["sync_requests"].insert(
                mapOf(
                    "request_type" to "NEW_CUSTOMER",
                    "payload" to customer,
                    "status" to "pending"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // In a real app, we'd handle rollback or retry
        }
    }
}
