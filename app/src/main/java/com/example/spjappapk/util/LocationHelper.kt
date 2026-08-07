package com.example.spjappapk.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationHelper(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient? = try {
        LocationServices.getFusedLocationProviderClient(context)
    } catch (e: Exception) {
        android.util.Log.e("LocationHelper", "Failed to initialize FusedLocationProviderClient", e)
        null
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val client = fusedLocationClient ?: return null
        return try {
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("LocationHelper", "Error getting current location", e)
            null
        }
    }
}
