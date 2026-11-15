package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ShopSettings
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.DokanRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryPeriod(val labelBn: String) {
    TODAY("আজ"),
    YESTERDAY("গতকাল"),
    LAST_7_DAYS("৭ দিন"),
    THIS_MONTH("এই মাস"),
    ALL("সব"),
    CUSTOM("কাস্টম")
}

enum class HistoryTypeFilter(val labelBn: String) {
    ALL("সব ধরন"),
    CREDIT_SALE("বাকি"),
    PAYMENT("জমা"),
    CASH_SALE("নগদ বিক্রি")
}

data class DailySalesUiState(
    val selectedDateKey: String = Formatters.todayDateKey(),
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val paymentCollected: Double = 0.0,
    val cashReceivedTotal: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netCashBalance: Double = 0.0,
    val dayTransactions: List<TransactionEntity> = emptyList(),
    val dayExpenses: List<ExpenseEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val settings: ShopSettings = ShopSettings()
)

data class TransactionHistoryUiState(
    val period: HistoryPeriod = HistoryPeriod.LAST_7_DAYS,
    val typeFilter: HistoryTypeFilter = HistoryTypeFilter.ALL,
    val searchQuery: String = "",
    val customStartDateKey: String = Formatters.daysAgoDateKey(7),
    val customEndDateKey: String = Formatters.todayDateKey(),
    val transactions: List<TransactionEntity> = emptyList(),
    val totalCreditSales: Double = 0.0,
    val totalPayments: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val settings: ShopSettings = ShopSettings()
)

class SalesHistoryViewModel(private val repository: DokanRepository) : ViewModel() {

    private val selectedDateKey = MutableStateFlow(Formatters.todayDateKey())
    private val historyPeriod = MutableStateFlow(HistoryPeriod.LAST_7_DAYS)
    private val historyTypeFilter = MutableStateFlow(HistoryTypeFilter.ALL)
    private val historySearch = MutableStateFlow("")
    private val customStart = MutableStateFlow(Formatters.daysAgoDateKey(7))
    private val customEnd = MutableStateFlow(Formatters.todayDateKey())

    val dailyState: StateFlow<DailySalesUiState> = combine(
        selectedDateKey,
        repository.transactionsFlow,
        repository.expensesFlow,
        repository.customersFlow,
        repository.settingsFlow
    ) { dateKey, allTxs, allExpenses, customers, settings ->
        val dayTxs = allTxs.filter { it.dateKey == dateKey }
        val dayExp = allExpenses.filter { it.dateKey == dateKey }

        val cashSales = dayTxs.filter { it.type == TransactionType.CASH_SALE }.sumOf { it.amount }
        val creditSales = dayTxs.filter { it.type == TransactionType.CREDIT_SALE }.sumOf { it.amount }
        val paymentCollected = dayTxs.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }
        val totalExpenses = dayExp.sumOf { it.amount }
        val cashReceived = cashSales + paymentCollected

        DailySalesUiState(
            selectedDateKey = dateKey,
            totalSales = cashSales + creditSales,
            cashSales = cashSales,
            creditSales = creditSales,
            paymentCollected = paymentCollected,
            cashReceivedTotal = cashReceived,
            totalExpenses = totalExpenses,
            netCashBalance = cashReceived - totalExpenses,
            dayTransactions = dayTxs,
            dayExpenses = dayExp,
            customers = customers,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailySalesUiState()
    )

    private data class HistoryFilterParams(
        val period: HistoryPeriod,
        val typeFilter: HistoryTypeFilter,
        val search: String,
        val startKey: String,
        val endKey: String
    )

    private val historyParamsFlow = combine(
        historyPeriod,
        historyTypeFilter,
        historySearch,
        customStart,
        customEnd
    ) { p, t, s, start, end ->
        HistoryFilterParams(p, t, s, start, end)
    }

    val historyState: StateFlow<TransactionHistoryUiState> = combine(
        repository.transactionsFlow,
        repository.settingsFlow,
        historyParamsFlow
    ) { allTxs, settings, params ->
        val today = Formatters.todayDateKey()
        val yesterday = Formatters.yesterdayDateKey()
        val sevenDaysAgo = Formatters.daysAgoDateKey(6)
        val monthStart = Formatters.currentMonthStartKey()

        val dateFiltered = allTxs.filter { tx ->
            when (params.period) {
                HistoryPeriod.TODAY -> tx.dateKey == today
                HistoryPeriod.YESTERDAY -> tx.dateKey == yesterday
                HistoryPeriod.LAST_7_DAYS -> tx.dateKey >= sevenDaysAgo && tx.dateKey <= today
                HistoryPeriod.THIS_MONTH -> tx.dateKey >= monthStart && tx.dateKey <= today
                HistoryPeriod.CUSTOM -> {
                    val lo = minOf(params.startKey, params.endKey)
                    val hi = maxOf(params.startKey, params.endKey)
                    tx.dateKey in lo..hi
                }
                HistoryPeriod.ALL -> true
            }
        }

        val typeFiltered = dateFiltered.filter { tx ->
            when (params.typeFilter) {
                HistoryTypeFilter.ALL -> true
                HistoryTypeFilter.CREDIT_SALE -> tx.type == TransactionType.CREDIT_SALE
                HistoryTypeFilter.PAYMENT -> tx.type == TransactionType.PAYMENT
                HistoryTypeFilter.CASH_SALE -> tx.type == TransactionType.CASH_SALE
            }
        }

        val cleanQ = params.search.trim().lowercase()
        val finalTxs = if (cleanQ.isEmpty()) {
            typeFiltered
        } else {
            typeFiltered.filter {
                it.customerName.lowercase().contains(cleanQ) ||
                    it.note.lowercase().contains(cleanQ)
            }
        }

        TransactionHistoryUiState(
            period = params.period,
            typeFilter = params.typeFilter,
            searchQuery = params.search,
            customStartDateKey = params.startKey,
            customEndDateKey = params.endKey,
            transactions = finalTxs,
            totalCreditSales = finalTxs.filter { it.type == TransactionType.CREDIT_SALE }.sumOf { it.amount },
            totalPayments = finalTxs.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount },
            totalCashSales = finalTxs.filter { it.type == TransactionType.CASH_SALE }.sumOf { it.amount },
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionHistoryUiState()
    )

    fun setSelectedDate(dateKey: String) {
        selectedDateKey.value = dateKey
    }

    fun shiftSelectedDate(deltaDays: Int) {
        selectedDateKey.value = Formatters.shiftDateKey(selectedDateKey.value, deltaDays)
    }

    fun setHistoryPeriod(period: HistoryPeriod) {
        historyPeriod.value = period
    }

    fun setHistoryTypeFilter(filter: HistoryTypeFilter) {
        historyTypeFilter.value = filter
    }

    fun setHistorySearch(query: String) {
        historySearch.value = query
    }

    fun setCustomDateRange(startKey: String, endKey: String) {
        customStart.value = startKey
        customEnd.value = endKey
        historyPeriod.value = HistoryPeriod.CUSTOM
    }

    fun recordDailySalesBundle(
        dateKey: String,
        cashSales: Double,
        creditSales: Double,
        customerId: Long?,
        expenseTitle: String,
        expenseAmount: Double,
        note: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                repository.recordDailySalesBundle(
                    dateKey = dateKey,
                    cashSales = cashSales,
                    creditSales = creditSales,
                    customerId = customerId,
                    expenseTitle = expenseTitle,
                    expenseAmount = expenseAmount,
                    note = note
                )
            )
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        note: String,
        dateKey: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val ts = if (dateKey == Formatters.todayDateKey()) {
            System.currentTimeMillis()
        } else {
            Formatters.parseDateKeyToMillis(dateKey) + 12 * 60 * 60 * 1000L
        }
        viewModelScope.launch {
            onResult(repository.addExpense(title, amount, note, ts))
        }
    }

    fun deleteExpense(expenseId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deleteExpense(expenseId))
        }
    }

    fun updateTransaction(
        transactionId: Long,
        newType: TransactionType,
        newAmount: Double,
        newNote: String,
        newTimestamp: Long,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.updateTransaction(transactionId, newType, newAmount, newNote, newTimestamp))
        }
    }

    fun deleteTransaction(transactionId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deleteTransaction(transactionId))
        }
    }
}
