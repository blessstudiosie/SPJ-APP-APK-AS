package com.example.spjappapk.data.repository

import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.remote.models.SalaryConfirmation
import com.example.spjappapk.data.remote.models.SalaryScheme
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SalaryRepository {

    suspend fun getSalaryScheme(salesPersonId: String, salesPersonName: String? = null): SalaryScheme? = withContext(Dispatchers.IO) {
        try {
            // Priority 1: Match sales_person_id
            if (salesPersonId.isNotBlank()) {
                val personalSchemes = SupabaseClient.client.postgrest["salary_schemes"].select {
                    filter { eq("sales_person_id", salesPersonId) }
                }.decodeList<SalaryScheme>()

                if (personalSchemes.isNotEmpty()) {
                    return@withContext personalSchemes.first()
                }
            }

            // Priority 2: Match sales_person_name
            if (!salesPersonName.isNullOrBlank()) {
                val nameSchemes = SupabaseClient.client.postgrest["salary_schemes"].select {
                    filter { ilike("sales_person_name", salesPersonName) }
                }.decodeList<SalaryScheme>()

                if (nameSchemes.isNotEmpty()) {
                    return@withContext nameSchemes.first()
                }
            }

            // Priority 3: Fallback to company default
            val defaultSchemes = SupabaseClient.client.postgrest["salary_schemes"].select {
                filter { eq("is_company_default", true) }
            }.decodeList<SalaryScheme>()

            return@withContext defaultSchemes.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getSalaryConfirmation(salesPersonId: String, periodMonth: String): SalaryConfirmation? = withContext(Dispatchers.IO) {
        try {
            val confirmations = SupabaseClient.client.postgrest["salary_confirmations"].select {
                filter {
                    eq("sales_person_id", salesPersonId)
                    eq("period_month", periodMonth)
                }
            }.decodeList<SalaryConfirmation>()
            
            return@withContext confirmations.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun confirmSalary(confirmation: SalaryConfirmation) = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.postgrest["salary_confirmations"].insert(confirmation)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPendingConfirmations(salesPersonId: String): List<SalaryConfirmation> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.postgrest["salary_confirmations"].select {
                filter {
                    eq("sales_person_id", salesPersonId)
                    eq("status", "PENDING")
                }
            }.decodeList<SalaryConfirmation>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun confirmSalaryReceipt(
        confirmationId: String,
        status: String = "CONFIRMED",
        signatureBase64: String,
        confirmedSource: String = "ANDROID",
        confirmedAt: String,
        confirmedByUserId: String,
        confirmedByName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val updatePayload = buildJsonObject {
                put("status", status)
                put("signature_image", signatureBase64)
                put("confirmed_source", confirmedSource)
                put("confirmed_at", confirmedAt)
                put("confirmed_by_user_id", confirmedByUserId)
                put("confirmed_by_name", confirmedByName)
            }

            SupabaseClient.client.postgrest["salary_confirmations"].update(updatePayload) {
                filter {
                    eq("id", confirmationId)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
