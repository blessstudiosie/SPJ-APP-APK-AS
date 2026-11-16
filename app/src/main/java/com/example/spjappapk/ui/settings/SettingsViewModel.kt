package com.example.spjappapk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.repository.CustomerRepository
import com.example.spjappapk.data.repository.InventoryRepository
import com.example.spjappapk.data.repository.SalesRepository
import com.example.spjappapk.data.repository.VisitRepository
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.data.remote.models.AppUpdate
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import android.net.Uri
import com.example.spjappapk.data.repository.BackupRepository

class SettingsViewModel(
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
    private val salesRepository: SalesRepository,
    private val visitRepository: VisitRepository,
    private val backupRepository: BackupRepository,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("")
    val syncStatus = _syncStatus.asStateFlow()

    private val _appUpdate = MutableStateFlow<AppUpdate?>(null)
    val appUpdate = _appUpdate.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun checkForUpdate() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Mengecek versi aplikasi..."
            try {
                val response = SupabaseClient.client.postgrest["app_versions"]
                    .select {
                        filter { eq("platform", "android") }
                        order("version_code", Order.DESCENDING)
                        limit(1)
                    }
                val updates = json.decodeFromString<List<AppUpdate>>(response.data)
                if (updates.isNotEmpty() && updates[0].versionCode > com.example.spjappapk.BuildConfig.VERSION_CODE) {
                    _appUpdate.value = updates[0]
                    _syncStatus.value = "Versi baru tersedia: ${updates[0].versionName}"
                } else {
                    _syncStatus.value = "Aplikasi sudah versi terbaru."
                }
            } catch (e: Exception) {
                _syncStatus.value = "Gagal cek update: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearUpdateState() {
        _appUpdate.value = null
    }

    fun fullResetAndSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                _syncStatus.value = "Membersihkan data lokal..."
                inventoryRepository.clearAndRefresh()
                
                _syncStatus.value = "Menarik data pelanggan..."
                customerRepository.clearAndRefresh()
                
                _syncStatus.value = "Menarik data tim (sales)..."
                salesRepository.refreshSalesPersons(isFullSync = true)

                _syncStatus.value = "Menarik data nota final..."
                if (currentUser != null) {
                    val isManagerOrOwner = currentUser.role?.lowercase() == "manager" || currentUser.role?.lowercase() == "owner"
                    salesRepository.clearAndRefreshSalesMaster(
                        currentUser.id,
                        isManagerOrOwner
                    )
                }

                _syncStatus.value = "Menarik data kunjungan..."
                if (currentUser != null) {
                    visitRepository.refreshVisits(currentUser.id, isFullSync = true)
                }
                
                _syncStatus.value = "Sinkronisasi selesai!"
            } catch (e: Exception) {
                _syncStatus.value = "Gagal: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun fixOrphanVisits() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Memindai antrean kunjungan..."
            try {
                val unsynced = visitRepository.allVisits.first().filter { 
                    it.syncStatus == "PENDING" || it.syncStatus == "ERROR" 
                }
                
                if (unsynced.isEmpty()) {
                    _syncStatus.value = "Tidak ada antrean bermasalah."
                } else {
                    _syncStatus.value = "Memperbaiki ${unsynced.size} data..."
                    unsynced.forEach { visit ->
                        customerRepository.checkAndFixOrphanCustomer(
                            visit.customerId,
                            visit.customerName,
                            visit.userId,
                            visit.salesPersonName
                        )
                    }
                    _syncStatus.value = "Perbaikan selesai. Menjalankan sinkronisasi..."
                    visitRepository.scheduleSync()
                }
            } catch (e: Exception) {
                _syncStatus.value = "Gagal perbaikan: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Membuat file backup data..."
            val result = backupRepository.createBackup(uri)
            result.fold(
                onSuccess = {
                    _syncStatus.value = "Berhasil! File backup berhasil disimpan."
                },
                onFailure = { err ->
                    _syncStatus.value = "Gagal membuat backup: ${err.localizedMessage}"
                }
            )
            _isSyncing.value = false
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Membaca & memulihkan data dari backup..."
            val result = backupRepository.restoreBackup(uri)
            result.fold(
                onSuccess = { res ->
                    _syncStatus.value = "Pemulihan Berhasil! (${res.visitsCount} Kunjungan, ${res.ordersCount} Pesanan, ${res.customersCount} Pelanggan, ${res.paymentsCount} Pembayaran)."
                    // Triggers background sync to send restored unsynced items
                    visitRepository.scheduleSync()
                    salesRepository.scheduleSync()
                },
                onFailure = { err ->
                    _syncStatus.value = "Gagal memulihkan backup: ${err.localizedMessage}"
                }
            )
            _isSyncing.value = false
        }
    }

    private fun md5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        return java.math.BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    fun changeMyPassword(
        oldPasswordRaw: String,
        newPasswordRaw: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = currentUser
        if (user == null) {
            onResult(false, "Sesi pengguna tidak ditemukan.")
            return
        }

        val cleanOld = oldPasswordRaw.trim()
        val cleanNew = newPasswordRaw.trim()

        if (cleanOld.isEmpty()) {
            onResult(false, "Password lama wajib diisi.")
            return
        }
        if (cleanNew.length < 4) {
            onResult(false, "Password baru minimal terdiri dari 4 karakter.")
            return
        }
        if (cleanNew.equals("123", ignoreCase = true) || cleanNew.equals("ganti123", ignoreCase = true)) {
            onResult(false, "Password baru tidak boleh menggunakan password bawaan ('123').")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val resp = SupabaseClient.client.postgrest["sales_persons"]
                    .select { filter { eq("id", user.id) } }
                val list = json.decodeFromString<List<SalesPerson>>(resp.data)
                val remoteUser = list.firstOrNull()
                val storedPass = remoteUser?.password?.trim() ?: user.password?.trim() ?: ""

                val isOldValid = if (storedPass.isEmpty()) {
                    cleanOld == "123"
                } else {
                    storedPass.equals(cleanOld, ignoreCase = true) ||
                    storedPass.equals(md5(cleanOld), ignoreCase = true)
                }

                if (!isOldValid) {
                    onResult(false, "Password lama yang Anda masukkan salah.")
                    return@launch
                }

                SupabaseClient.client.postgrest["sales_persons"].update(
                    mapOf("password" to cleanNew)
                ) {
                    filter { eq("id", user.id) }
                }

                _syncStatus.value = "Password berhasil diperbarui!"
                onResult(true, "Password Anda berhasil diperbarui.")
            } catch (e: Exception) {
                onResult(false, "Gagal mengubah password: ${e.localizedMessage ?: "Periksa koneksi internet Anda"}")
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
