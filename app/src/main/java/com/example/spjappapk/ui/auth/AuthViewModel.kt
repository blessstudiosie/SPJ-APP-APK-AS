package com.example.spjappapk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.session.SessionManager
import com.example.spjappapk.util.DateTimeUtil
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
    val role: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("resign_date") val resignDate: String? = null,
    @SerialName("resign_reason") val resignReason: String? = null
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
        // Load session automatically and validate if user account is still active
        viewModelScope.launch {
            sessionManager.userFlow.collectLatest { savedUser ->
                if (savedUser != null) {
                    validateSession(savedUser)
                } else {
                    _user.value = null
                    _isInitialLoading.value = false
                }
            }
        }
    }

    private suspend fun validateSession(savedUser: SalesPerson) {
        try {
            val response = SupabaseClient.client.postgrest["sales_persons"]
                .select {
                    filter { eq("id", savedUser.id) }
                }
            val data = json.parseToJsonElement(response.data).jsonArray
            if (data.isNotEmpty()) {
                val latestUser = json.decodeFromJsonElement<SalesPerson>(data[0].jsonObject)
                if (!latestUser.isActive) {
                    // Account deactivated by Admin! Auto-logout
                    sessionManager.clearSession()
                    _user.value = null
                    val pesanResign = if (!latestUser.resignDate.isNullOrBlank()) {
                        val formattedDate = try {
                            DateTimeUtil.formatDateOnly(
                                DateTimeUtil.parseIso(latestUser.resignDate)?.time ?: 0L
                            )
                        } catch (e: Exception) { latestUser.resignDate }
                        " per tanggal $formattedDate"
                    } else ""
                    val reasonText = if (!latestUser.resignReason.isNullOrBlank()) " (${latestUser.resignReason})" else ""
                    _error.value = "Sesi Anda telah berakhir karena akun ini telah dinonaktifkan oleh Administrator$pesanResign$reasonText."
                    _isInitialLoading.value = false
                    return
                } else {
                    _user.value = latestUser
                    sessionManager.saveSession(latestUser)
                }
            } else {
                _user.value = savedUser
            }
        } catch (e: Exception) {
            // Offline fallback: use saved session
            _user.value = savedUser
        } finally {
            _isInitialLoading.value = false
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
                    
                    // Cek status keaktifan akun (Is Active Check)
                    if (!userObj.isActive) {
                        val pesanResign = if (!userObj.resignDate.isNullOrBlank()) {
                            val formattedDate = try {
                                DateTimeUtil.formatDateOnly(
                                    DateTimeUtil.parseIso(userObj.resignDate)?.time ?: 0L
                                )
                            } catch (e: Exception) { userObj.resignDate }
                            " per tanggal $formattedDate"
                        } else ""
                        val reasonText = if (!userObj.resignReason.isNullOrBlank()) " (${userObj.resignReason})" else ""
                        _error.value = "Akun Anda telah dinonaktifkan / resign$pesanResign$reasonText. Silakan hubungi Admin kantor CV. Sarana Prima Jaya."
                        return@launch
                    }

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
