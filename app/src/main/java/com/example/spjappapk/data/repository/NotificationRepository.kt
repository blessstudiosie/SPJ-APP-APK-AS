package com.example.spjappapk.data.repository

import com.example.spjappapk.data.local.dao.NotificationDao
import com.example.spjappapk.data.local.entities.Notification
import com.example.spjappapk.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val notificationDao: NotificationDao
) {
    val allNotifications: Flow<List<Notification>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun refreshNotifications(userId: String) = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.client.postgrest["notifications"]
                .select {
                    filter {
                        or {
                            eq("user_id", userId)
                            // Allow broadcast notifications where user_id is null
                            // eq("user_id", null) 
                        }
                    }
                    limit(5000)
                }
            val data = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<List<Notification>>(response.data)
            
            notificationDao.insertAll(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        try {
            notificationDao.markAsRead(notificationId)
            // Push status to server
            SupabaseClient.client.postgrest["notifications"].update(
                mapOf("is_read" to true)
            ) {
                filter { eq("id", notificationId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllAsRead(userId: String) = withContext(Dispatchers.IO) {
        try {
            notificationDao.markAllAsRead()
            SupabaseClient.client.postgrest["notifications"].update(
                mapOf("is_read" to true)
            ) {
                filter { eq("user_id", userId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
