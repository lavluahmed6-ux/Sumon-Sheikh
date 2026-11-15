package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.DokanTheme
import com.example.ui.viewmodel.HistoryPeriod
import com.example.ui.viewmodel.HistoryTypeFilter
import com.example.ui.viewmodel.TransactionHistoryUiState
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: TransactionHistoryUiState,
    onBack: () -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit,
    onTypeFilterChange: (HistoryTypeFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onCustomRangeSelected: (String, String) -> Unit,
    onUpdateTransaction: (TransactionEntity, TransactionType, Double, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val dokanColors = DokanTheme.colors
    val sym = uiState.settings.currencySymbol
    var showDateRangePicker by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var deletingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("সম্পূর্ণ লেনদেনের ইতিহাস") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("কাস্টমারের নাম বা নোট দিয়ে খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "পরিষ্কার করুন")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Period Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HistoryPeriod.entries) { period ->
                    FilterChip(
                        selected = uiState.period == period,
                        onClick = {
                            if (period == HistoryPeriod.CUSTOM) {
                                showDateRangePicker = true
                            } else {
                                onPeriodChange(period)
                            }
                        },
                        label = {
                            val label = if (period == HistoryPeriod.CUSTOM && uiState.period == HistoryPeriod.CUSTOM) {
                                "${Formatters.formatShortDate(uiState.customStartDateKey)}-${Formatters.formatShortDate(uiState.customEndDateKey)}"
                            } else {
                                period.labelBn
                            }
                            Text(label)
                        },
                        modifier = Modifier.testTag("history_period_${period.name}")
                    )
                }
            }

            // Type Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HistoryTypeFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.typeFilter == filter,
                        onClick = { onTypeFilterChange(filter) },
                        label = { Text(filter.labelBn) },
                        modifier = Modifier.testTag("history_type_${filter.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtered Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = dokanColors.salesBlueContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("নগদ বিক্রি", style = MaterialTheme.typography.labelSmall, color = dokanColors.salesBlue)
                        Text(
                            Formatters.formatCurrency(uiState.totalCashSales, sym),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = dokanColors.salesBlue
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = dokanColors.dueRedContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("বাকি বিক্রি", style = MaterialTheme.typography.labelSmall, color = dokanColors.dueRed)
                        Text(
                            Formatters.formatCurrency(uiState.totalCreditSales, sym),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = dokanColors.dueRed
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = dokanColors.paymentGreenContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("টাকা জমা", style = MaterialTheme.typography.labelSmall, color = dokanColors.paymentGreen)
                        Text(
                            Formatters.formatCurrency(uiState.totalPayments, sym),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = dokanColors.paymentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.transactions.isEmpty()) {
                EmptyStateView(
                    title = "কোনো লেনদেন পাওয়া যায়নি",
                    subtitle = "নির্বাচিত সময় বা ফিল্টারে কোনো লেনদেন নেই।",
                    icon = Icons.Outlined.ReceiptLong
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { tx ->
                        TransactionRowCard(
                            transaction = tx,
                            currencySymbol = sym,
                            showActions = true,
                            onEdit = { editingTx = tx },
                            onDelete = { deletingTx = tx }
                        )
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        val rangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = Formatters.parseDateKeyToMillis(uiState.customStartDateKey) + 12 * 60 * 60 * 1000L,
            initialSelectedEndDateMillis = Formatters.parseDateKeyToMillis(uiState.customEndDateKey) + 12 * 60 * 60 * 1000L
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    enabled = rangeState.selectedStartDateMillis != null && rangeState.selectedEndDateMillis != null,
                    onClick = {
                        val s = rangeState.selectedStartDateMillis
                        val e = rangeState.selectedEndDateMillis
                        if (s != null && e != null) {
                            onCustomRangeSelected(
                                Formatters.dateKeyFromMillis(s),
                                Formatters.dateKeyFromMillis(e)
                            )
                        }
                        showDateRangePicker = false
                    }
                ) { Text("প্রয়োগ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("বাতিল") }
            }
        ) {
            DateRangePicker(
                state = rangeState,
                modifier = Modifier.height(440.dp)
            )
        }
    }

    editingTx?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            currencySymbol = sym,
            onDismiss = { editingTx = null },
            onSubmit = { type, amt, note ->
                editingTx = null
                onUpdateTransaction(tx, type, amt, note)
            }
        )
    }

    deletingTx?.let { tx ->
        ConfirmDeleteDialog(
            title = "লেনদেন মুছে ফেলবেন?",
            message = "${tx.customerName}-এর ${tx.type.labelBn} (${Formatters.formatCurrency(tx.amount, sym)}) মুছে ফেললে সংশ্লিষ্ট বাকি ও রিপোর্ট স্বয়ংক্রিয়ভাবে আপডেট হবে।",
            onConfirm = {
                val id = tx.id
                deletingTx = null
                onDeleteTransaction(id)
            },
            onDismiss = { deletingTx = null }
        )
    }
}
