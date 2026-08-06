package com.example.spjappapk.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

object SupabaseClient {
    private const val SUPABASE_URL = "https://ntvyrjrqxpmecqogolni.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im50dnlyanJxeHBtZWNxb2dvbG5pIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU1MjE0MTYsImV4cCI6MjEwMTA5NzQxNn0.3Wjjj-hbQPE7VGAy-15x182vG6aU3dtkHMW9i_k5KwI"

    val client by lazy {
        try {
            createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_ANON_KEY
            ) {
                httpEngine = OkHttp.create()
                install(Auth)
                install(Postgrest) {
                    serializer = io.github.jan.supabase.serializer.KotlinXSerializer(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
                install(Storage)
                install(Realtime)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
