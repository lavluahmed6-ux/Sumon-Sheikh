package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ShopSettings
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.DokanRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val settings: ShopSettings = ShopSettings(),
    val todayTotalSales: Double = 0.0,
    val todayCashReceived: Double = 0.0,
    val todayCreditSales: Double = 0.0,
    val totalStoreDue: Double = 0.0,
    val totalCustomers: Int = 0,
    val todayTransactionCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(repository: DokanRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.customersFlow,
        repository.transactionsFlow,
        repository.settingsFlow
    ) { customers, transactions, settings ->
        val today = Formatters.todayDateKey()
        val todayTxs = transactions.filter { it.dateKey == today }

        val todayCashSales = todayTxs
            .filter { it.type == TransactionType.CASH_SALE }
            .sumOf { it.amount }
        val todayCreditSales = todayTxs
            .filter { it.type == TransactionType.CREDIT_SALE }
            .sumOf { it.amount }
        val todayDuePayments = todayTxs
            .filter { it.type == TransactionType.PAYMENT }
            .sumOf { it.amount }

        HomeUiState(
            settings = settings,
            todayTotalSales = todayCashSales + todayCreditSales,
            todayCashReceived = todayCashSales + todayDuePayments,
            todayCreditSales = todayCreditSales,
            totalStoreDue = customers.sumOf { it.currentDue },
            totalCustomers = customers.size,
            todayTransactionCount = todayTxs.size,
            recentTransactions = transactions.take(8),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
