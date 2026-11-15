package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.DokanBackupPayload
import com.example.data.local.ShopSettings
import com.example.data.repository.DokanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: ShopSettings = ShopSettings(),
    val customerCount: Int = 0,
    val transactionCount: Int = 0,
    val expenseCount: Int = 0,
    val hasLocalBackup: Boolean = false,
    val pendingRestorePayload: DokanBackupPayload? = null,
    val isProcessingBackup: Boolean = false
)

class SettingsViewModel(
    private val repository: DokanRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val pendingRestore = MutableStateFlow<DokanBackupPayload?>(null)
    private val isProcessing = MutableStateFlow(false)
    private val localBackupExists = MutableStateFlow(backupManager.hasLocalBackup())

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settingsFlow,
        repository.customersFlow,
        repository.transactionsFlow,
        repository.expensesFlow,
        pendingRestore,
        isProcessing,
        localBackupExists
    ) { values ->
        val settings = values[0] as ShopSettings
        val customers = values[1] as List<*>
        val transactions = values[2] as List<*>
        val expenses = values[3] as List<*>
        val pending = values[4] as DokanBackupPayload?
        val busy = values[5] as Boolean
        val hasLocal = values[6] as Boolean

        SettingsUiState(
            settings = settings,
            customerCount = customers.size,
            transactionCount = transactions.size,
            expenseCount = expenses.size,
            hasLocalBackup = hasLocal,
            pendingRestorePayload = pending,
            isProcessingBackup = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(hasLocalBackup = backupManager.hasLocalBackup())
    )

    fun updateShopProfile(name: String, phone: String, address: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.updateShopProfile(name, phone, address)
            onDone()
        }
    }

    fun updateCurrency(symbol: String) {
        viewModelScope.launch {
            repository.updateCurrency(symbol)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }

    fun createLocalBackup(onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            val res = backupManager.createLocalBackup()
            localBackupExists.value = backupManager.hasLocalBackup()
            isProcessing.value = false
            onResult(res)
        }
    }

    fun exportBackupToUri(uri: Uri, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            val res = backupManager.exportBackupToUri(uri)
            localBackupExists.value = backupManager.hasLocalBackup()
            isProcessing.value = false
            onResult(res)
        }
    }

    fun prepareRestoreFromUri(uri: Uri, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            val res = backupManager.parseBackupFromUri(uri)
            isProcessing.value = false
            res.onSuccess { pendingRestore.value = it }.onFailure(onError)
        }
    }

    fun prepareRestoreFromLocal(onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            val res = backupManager.parseLocalBackup()
            isProcessing.value = false
            res.onSuccess { pendingRestore.value = it }.onFailure(onError)
        }
    }

    fun dismissRestorePreview() {
        pendingRestore.value = null
    }

    fun confirmRestore(onResult: (Result<Unit>) -> Unit) {
        val payload = pendingRestore.value ?: return
        viewModelScope.launch {
            isProcessing.value = true
            val res = backupManager.restorePayload(payload)
            pendingRestore.value = null
            isProcessing.value = false
            onResult(res)
        }
    }
}
