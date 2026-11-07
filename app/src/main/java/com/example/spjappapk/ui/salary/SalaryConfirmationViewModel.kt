package com.example.spjappapk.ui.salary

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.data.remote.models.SalaryConfirmation
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.util.DateTimeUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

data class SalaryConfirmationUiState(
    val isLoading: Boolean = false,
    val pendingConfirmations: List<SalaryConfirmation> = emptyList(),
    val selectedConfirmation: SalaryConfirmation? = null,
    val isSignatureEmpty: Boolean = true,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class SalaryConfirmationViewModel(
    private val app: SPJApplication,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalaryConfirmationUiState())
    val uiState: StateFlow<SalaryConfirmationUiState> = _uiState.asStateFlow()

    private var currentBitmapProvider: (() -> Bitmap?)? = null

    init {
        loadPendingConfirmations()
    }

    fun loadPendingConfirmations() {
        if (currentUser == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
            try {
                val list = app.salaryRepository.getPendingConfirmations(currentUser.id)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        pendingConfirmations = list,
                        selectedConfirmation = if (list.isNotEmpty()) list.first() else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun selectConfirmation(confirmation: SalaryConfirmation) {
        _uiState.update { it.copy(selectedConfirmation = confirmation) }
    }

    fun updateSignatureState(isEmpty: Boolean, bitmapProvider: () -> Bitmap?) {
        currentBitmapProvider = bitmapProvider
        _uiState.update { it.copy(isSignatureEmpty = isEmpty) }
    }

    fun confirmAndSignReceipt() {
        val selected = _uiState.value.selectedConfirmation ?: return
        if (currentUser == null) return

        val bitmap = currentBitmapProvider?.invoke()
        if (bitmap == null) {
            _uiState.update { it.copy(error = "Tanda tangan tidak boleh kosong!") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val base64Signature = bitmap.toBase64Png()
                val nowIso = DateTimeUtil.getUtcIsoFormat().format(Date())

                val success = app.salaryRepository.confirmSalaryReceipt(
                    confirmationId = selected.id,
                    status = "CONFIRMED",
                    signatureBase64 = base64Signature,
                    confirmedSource = "ANDROID",
                    confirmedAt = nowIso,
                    confirmedByUserId = currentUser.id,
                    confirmedByName = currentUser.name
                )

                if (success) {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    loadPendingConfirmations()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Gagal mengirim konfirmasi ke server.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}
