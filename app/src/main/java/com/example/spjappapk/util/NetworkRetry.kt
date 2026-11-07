package com.example.spjappapk.util

import android.util.Log
import kotlinx.coroutines.delay

suspend fun <T> retryNetworkCall(
    times: Int = 3,
    initialDelay: Long = 1000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            Log.w("NetworkRetry", "Network call attempt ${attempt + 1} failed: ${e.message}. Retrying in ${currentDelay}ms...")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    return block()
}
