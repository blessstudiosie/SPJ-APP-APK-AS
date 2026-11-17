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
import kotlinx.coroutines.flow.firstOrNull
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
    val password: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("resign_date") val resignDate: String? = null,
    @SerialName("resign_reason") val resignReason: String? = null
)

class AuthViewModel(
    private val sessionManager: SessionManager,
    private val salesPersonDao: com.example.spjappapk.data.local.dao.SalesPersonDao? = null
) : ViewModel() {
    private val _user = MutableStateFlow<SalesPerson?>(null)
    val user = _user.asStateFlow()

    private var _pendingPasswordChangeUser: SalesPerson? = null

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
            // Offline fallback: cek juga status aktif/resign di database lokal Room agar user resign tetap terblokir walau offline
            val localSp = try {
                salesPersonDao?.getSalesPersonById(savedUser.id)
            } catch (_: Exception) {
                null
            }
            if (localSp != null && (!localSp.isActive || localSp.employmentStatus.equals("RESIGN", ignoreCase = true))) {
                sessionManager.clearSession()
                _user.value = null
                _error.value = "Sesi Anda telah berakhir karena akun ini berstatus Resign / nonaktif."
            } else {
                _user.value = savedUser
            }
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
                val cleanName = name.trim()
                val cleanPass = passwordRaw.trim()
                val passwordMd5 = md5(cleanPass)
                
                // Tarik berdasarkan nama, lalu verifikasi password (mendukung plain, MD5, atau null default '123')
                val response = SupabaseClient.client.postgrest["sales_persons"]
                    .select {
                        filter {
                            ilike("name", cleanName)
                        }
                    }
                
                val data = json.parseToJsonElement(response.data).jsonArray
                if (data.isNotEmpty()) {
                    val userObj = json.decodeFromJsonElement<SalesPerson>(data[0].jsonObject)
                    val storedPass = userObj.password?.trim() ?: ""

                    val isPasswordValid = if (storedPass.isEmpty()) {
                        cleanPass == "123" || cleanPass.equals("ganti123", ignoreCase = true)
                    } else {
                        storedPass.equals(cleanPass, ignoreCase = true) ||
                        storedPass.equals(passwordMd5, ignoreCase = true)
                    }

                    if (!isPasswordValid) {
                        _error.value = "Login Gagal: Nama atau Password salah."
                        return@launch
                    }
                    
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

                    // Cek jika password masih default ('123' atau 'ganti123' atau kosong)
                    val isDefaultPass = cleanPass == "123" ||
                        cleanPass.equals("ganti123", ignoreCase = true) ||
                        storedPass.isEmpty() ||
                        storedPass == "123" ||
                        storedPass.equals(md5("123"), ignoreCase = true)

                    if (isDefaultPass) {
                        _pendingPasswordChangeUser = userObj
                        _needsPasswordChange.value = true
                    } else {
                        // Simpan ke session manager agar "Stay Logged In"
                        _user.value = userObj
                        sessionManager.saveSession(userObj)
                    }
                } else {
                    _error.value = "Login Gagal: Nama atau Password salah."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback Offline Login menggunakan data lokal Room (sales_persons_profile)
                val cleanName = name.trim()
                val cleanPass = passwordRaw.trim()
                val passwordMd5 = md5(cleanPass)
                val localList = try {
                    salesPersonDao?.getAllSalesPersons()?.firstOrNull() ?: emptyList()
                } catch (dbEx: Exception) {
                    emptyList()
                }
                val localMatch = localList.find { it.name.trim().equals(cleanName, ignoreCase = true) }
                if (localMatch != null) {
                    val storedPass = localMatch.password?.trim() ?: ""
                    val isPasswordValid = if (storedPass.isEmpty()) {
                        cleanPass == "123" || cleanPass.equals("ganti123", ignoreCase = true)
                    } else {
                        storedPass.equals(cleanPass, ignoreCase = true) ||
                        storedPass.equals(passwordMd5, ignoreCase = true)
                    }

                    if (!isPasswordValid) {
                        _error.value = "Login Offline Gagal: Nama atau Password salah."
                        return@launch
                    }

                    val empStatus = localMatch.employmentStatus?.trim()?.uppercase() ?: "AKTIF"
                    if (!localMatch.isActive || empStatus == "RESIGN" || empStatus == "NONAKTIF") {
                        _error.value = "Akun Anda telah dinonaktifkan / resign. Silakan hubungi Admin kantor CV. Sarana Prima Jaya."
                        return@launch
                    }

                    val isDefaultPass = cleanPass == "123" ||
                        cleanPass.equals("ganti123", ignoreCase = true) ||
                        storedPass.isEmpty() ||
                        storedPass == "123" ||
                        storedPass.equals(md5("123"), ignoreCase = true)

                    if (isDefaultPass) {
                        _error.value = "Anda wajib terhubung ke internet pada login pertama untuk mengganti password bawaan ('123')."
                        return@launch
                    }

                    val offlineUser = SalesPerson(
                        id = localMatch.id,
                        name = localMatch.name,
                        phone = localMatch.phone,
                        email = localMatch.email,
                        targetOmset = localMatch.targetOmset,
                        role = localMatch.role,
                        password = localMatch.password,
                        isActive = localMatch.isActive,
                        resignDate = localMatch.resignDate,
                        resignReason = localMatch.resignReason
                    )
                    _user.value = offlineUser
                    sessionManager.saveSession(offlineUser)
                } else {
                    _error.value = "Koneksi Gagal (Offline): Data akun belum tersimpan di HP ini. Hubungkan ke internet untuk login pertama kali."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            val targetUser = _pendingPasswordChangeUser ?: _user.value ?: return@launch
            val cleanNew = newPassword.trim()
            if (cleanNew.length < 4) {
                _error.value = "Password baru minimal terdiri dari 4 karakter."
                return@launch
            }
            if (cleanNew.equals("123", ignoreCase = true) || cleanNew.equals("ganti123", ignoreCase = true)) {
                _error.value = "Password baru tidak boleh menggunakan password bawaan ('123')."
                return@launch
            }

            _isLoading.value = true
            _error.value = null
            try {
                SupabaseClient.client.postgrest["sales_persons"].update(
                    mapOf("password" to cleanNew)
                ) {
                    filter { eq("id", targetUser.id) }
                }
                
                val updatedUser = targetUser.copy(password = cleanNew)
                _pendingPasswordChangeUser = null
                _needsPasswordChange.value = false
                _user.value = updatedUser
                sessionManager.saveSession(updatedUser)
            } catch (e: Exception) {
                _error.value = "Gagal ubah password: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelPasswordChange() {
        _pendingPasswordChangeUser = null
        _needsPasswordChange.value = false
        _error.value = null
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
