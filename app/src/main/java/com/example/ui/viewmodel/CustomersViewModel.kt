package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.ShopSettings
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.DokanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DueSortOption(val labelBn: String) {
    HIGHEST_FIRST("সর্বোচ্চ বাকি"),
    LOWEST_FIRST("সর্বনিম্ন বাকি"),
    NAME_ASC("নাম (A-Z)")
}

data class CustomersUiState(
    val allCustomers: List<CustomerEntity> = emptyList(),
    val filteredCustomers: List<CustomerEntity> = emptyList(),
    val searchQuery: String = "",
    val dueCustomers: List<CustomerEntity> = emptyList(),
    val dueSearchQuery: String = "",
    val dueSortOption: DueSortOption = DueSortOption.HIGHEST_FIRST,
    val totalOutstandingDue: Double = 0.0,
    val settings: ShopSettings = ShopSettings()
)

class CustomersViewModel(private val repository: DokanRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val dueSearchQuery = MutableStateFlow("")
    private val dueSortOption = MutableStateFlow(DueSortOption.HIGHEST_FIRST)

    val uiState: StateFlow<CustomersUiState> = combine(
        repository.customersFlow,
        repository.settingsFlow,
        searchQuery,
        dueSearchQuery,
        dueSortOption
    ) { customers, settings, query, dQuery, sort ->
        val cleanQ = query.trim().lowercase()
        val filtered = if (cleanQ.isEmpty()) {
            customers
        } else {
            customers.filter {
                it.name.lowercase().contains(cleanQ) || it.phone.lowercase().contains(cleanQ)
            }
        }

        val cleanDQ = dQuery.trim().lowercase()
        val withDue = customers.filter { it.currentDue > 0.0 }
            .filter {
                cleanDQ.isEmpty() ||
                    it.name.lowercase().contains(cleanDQ) ||
                    it.phone.lowercase().contains(cleanDQ)
            }
            .let { list ->
                when (sort) {
                    DueSortOption.HIGHEST_FIRST -> list.sortedByDescending { it.currentDue }
                    DueSortOption.LOWEST_FIRST -> list.sortedBy { it.currentDue }
                    DueSortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                }
            }

        CustomersUiState(
            allCustomers = customers,
            filteredCustomers = filtered,
            searchQuery = query,
            dueCustomers = withDue,
            dueSearchQuery = dQuery,
            dueSortOption = sort,
            totalOutstandingDue = customers.sumOf { it.currentDue },
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomersUiState()
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setDueSearchQuery(query: String) {
        dueSearchQuery.value = query
    }

    fun setDueSortOption(option: DueSortOption) {
        dueSortOption.value = option
    }

    fun observeCustomer(customerId: Long): Flow<CustomerEntity?> =
        repository.observeCustomer(customerId)

    fun observeCustomerTransactions(customerId: Long): Flow<List<TransactionEntity>> =
        repository.observeCustomerTransactions(customerId)

    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        previousDue: Double,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.addCustomer(name, phone, address, previousDue))
        }
    }

    fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String,
        address: String,
        previousDue: Double,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.updateCustomer(customerId, name, phone, address, previousDue))
        }
    }

    fun deleteCustomer(customerId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deleteCustomer(customerId))
        }
    }

    fun recordCreditPurchase(
        customerId: Long,
        totalPurchaseAmount: Double,
        paidNowAmount: Double,
        note: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.recordCreditPurchase(customerId, totalPurchaseAmount, paidNowAmount, note))
        }
    }

    fun recordPayment(
        customerId: Long,
        amount: Double,
        note: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.recordPayment(customerId, amount, note))
        }
    }

    fun recordCashSale(
        customerId: Long?,
        amount: Double,
        note: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.recordCashSale(customerId, amount, note))
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
