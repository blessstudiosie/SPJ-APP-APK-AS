package com.example.spjappapk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.session.SessionManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.math.BigInteger
import java.security.MessageDigest

@Serializable
data class SalesPerson(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("target_omset") val targetOmset: Double? = 0.0,
    val role: String? = null
)

class AuthViewModel(private val sessionManager: SessionManager) : ViewModel() {
    private val _user = MutableStateFlow<SalesPerson?>(null)
    val user = _user.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading = _isInitialLoading.asStateFlow()

    private val _needsPasswordChange = MutableStateFlow(false)
    val needsPasswordChange = _needsPasswordChange.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val json = Json { 
        ignoreUnknownKeys = true 
    }

    init {
        // Load session automatically
        viewModelScope.launch {
            sessionManager.userFlow.collectLatest { savedUser ->
                _user.value = savedUser
                _isInitialLoading.value = false
            }
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    fun login(name: String, passwordRaw: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val passwordMd5 = md5(passwordRaw)
                
                // Verifikasi langsung ke tabel sales_persons menggunakan NAMA dan mendukung MD5/Plain
                val response = SupabaseClient.client.postgrest["sales_persons"]
                    .select {
                        filter {
                            eq("name", name)
                            or {
                                eq("password", passwordRaw)
                                eq("password", passwordMd5)
                            }
                        }
                    }
                
                val data = json.parseToJsonElement(response.data).jsonArray
                if (data.isNotEmpty()) {
                    val userObj = json.decodeFromJsonElement<SalesPerson>(data[0].jsonObject)
                    
                    // Simpan ke session manager agar "Stay Logged In"
                    sessionManager.saveSession(userObj)
                    
                    // Cek jika password masih default
                    if (passwordRaw == "ganti123") {
                        _needsPasswordChange.value = true
                    }
                } else {
                    _error.value = "Login Gagal: Nama atau Password salah."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Koneksi Gagal: ${e.message ?: "Terjadi kesalahan"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch
            _isLoading.value = true
            try {
                SupabaseClient.client.postgrest["sales_persons"].update(
                    mapOf("password" to newPassword)
                ) {
                    filter { eq("id", currentUser.id) }
                }
                
                _needsPasswordChange.value = false
                _error.value = "Password berhasil diubah!"
            } catch (e: Exception) {
                _error.value = "Gagal ubah password: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _user.value = null
        }
    }
}
