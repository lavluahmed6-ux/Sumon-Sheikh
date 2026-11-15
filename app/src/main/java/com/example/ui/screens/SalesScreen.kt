package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ExpenseEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DailySalesBundleDialog
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ExpenseDialog
import com.example.ui.components.SummaryMetricCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.DokanTheme
import com.example.ui.viewmodel.DailySalesUiState
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    uiState: DailySalesUiState,
    onSelectDate: (String) -> Unit,
    onShiftDate: (Int) -> Unit,
    onOpenCashSale: () -> Unit,
    onOpenCreditSale: () -> Unit,
    onRecordDailyBundle: (
        cashSales: Double,
        creditSales: Double,
        customerId: Long?,
        expenseTitle: String,
        expenseAmount: Double,
        note: String
    ) -> Unit,
    onAddExpense: (title: String, amount: Double, note: String) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onUpdateTransaction: (TransactionEntity, TransactionType, Double, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dokanColors = DokanTheme.colors
    val sym = uiState.settings.currencySymbol
    val isToday = uiState.selectedDateKey == Formatters.todayDateKey()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDailyBundleDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var deletingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var deletingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "দৈনিক বিক্রি ও খরচ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onNavigateHistory,
                        modifier = Modifier.testTag("sales_to_history_btn")
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ইতিহাস")
                    }
                    TextButton(
                        onClick = onNavigateReports,
                        modifier = Modifier.testTag("sales_to_reports_btn")
                    ) {
                        Icon(Icons.Outlined.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("রিপোর্ট")
                    }
                }
            }
        }

        // Date Picker Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, dokanColors.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onShiftDate(-1) },
                        modifier = Modifier.testTag("sales_prev_day")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "আগের দিন")
                    }

                    Row(
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("sales_date_picker_trigger"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Formatters.formatDateBn(uiState.selectedDateKey),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { onShiftDate(1) },
                        enabled = !isToday,
                        modifier = Modifier.testTag("sales_next_day")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "পরের দিন")
                    }
                }
                if (!isToday) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { onSelectDate(Formatters.todayDateKey()) }) {
                            Text("আজকের তারিখে ফিরে যান")
                        }
                    }
                }
            }
        }

        // Net Cash Balance Highlight Card
        item {
            val netPositive = uiState.netCashBalance >= 0.0
            val netAccent = if (netPositive) dokanColors.paymentGreen else dokanColors.dueRed
            val netBg = if (netPositive) dokanColors.paymentGreenContainer else dokanColors.dueRedContainer

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = netBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "দৈনিক নিট নগদ ব্যালেন্স",
                            style = MaterialTheme.typography.titleSmall,
                            color = netAccent
                        )
                        Text(
                            text = "নগদ জমা (${Formatters.formatCurrency(uiState.cashReceivedTotal, sym)}) − খরচ (${Formatters.formatCurrency(uiState.totalExpenses, sym)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = netAccent.copy(alpha = 0.85f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = netAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Formatters.formatCurrency(uiState.netCashBalance, sym),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = netAccent
                        )
                    }
                }
            }
        }

        // 4 Daily Sales & Expense Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = "মোট বিক্রি",
                        value = Formatters.formatCurrency(uiState.totalSales, sym),
                        icon = Icons.Outlined.PointOfSale,
                        accentColor = dokanColors.salesBlue,
                        containerColor = dokanColors.salesBlueContainer,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "নগদ জমা (বিক্রি+আদায়)",
                        value = Formatters.formatCurrency(uiState.cashReceivedTotal, sym),
                        icon = Icons.Outlined.Payments,
                        accentColor = dokanColors.paymentGreen,
                        containerColor = dokanColors.paymentGreenContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = "বাকি বিক্রি",
                        value = Formatters.formatCurrency(uiState.creditSales, sym),
                        icon = Icons.Outlined.AddShoppingCart,
                        accentColor = dokanColors.dueRed,
                        containerColor = dokanColors.dueRedContainer,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "দৈনিক খরচ",
                        value = Formatters.formatCurrency(uiState.totalExpenses, sym),
                        icon = Icons.Outlined.MoneyOff,
                        accentColor = dokanColors.expenseAmber,
                        containerColor = dokanColors.expenseContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showDailyBundleDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = dokanColors.salesBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_daily_sales_bundle_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("দৈনিক বিক্রি ও খরচ একসাথে যোগ করুন")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenCashSale,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sales_cash_btn")
                    ) {
                        Text("নগদ বিক্রি", color = dokanColors.salesBlue)
                    }
                    OutlinedButton(
                        onClick = onOpenCreditSale,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sales_credit_btn")
                    ) {
                        Text("বাকি বিক্রি", color = dokanColors.dueRed)
                    }
                    OutlinedButton(
                        onClick = { showExpenseDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sales_expense_btn")
                    ) {
                        Text("+ খরচ", color = dokanColors.expenseAmber)
                    }
                }
            }
        }

        // Day Transactions
        item {
            Text(
                text = "এই দিনের লেনদেন (${uiState.dayTransactions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.dayTransactions.isEmpty()) {
            item {
                EmptyStateView(
                    title = "এই তারিখে কোনো বিক্রি বা জমা নেই",
                    subtitle = "নতুন বিক্রি বা দৈনিক হিসাব যোগ করতে উপরের বাটন ব্যবহার করুন।",
                    icon = Icons.Outlined.ReceiptLong
                )
            }
        } else {
            items(uiState.dayTransactions, key = { "tx_${it.id}" }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    currencySymbol = sym,
                    showActions = true,
                    onEdit = { editingTx = tx },
                    onDelete = { deletingTx = tx }
                )
            }
        }

        // Day Expenses
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "এই দিনের খরচসমূহ (${uiState.dayExpenses.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.dayExpenses.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "এই তারিখে কোনো খরচ যোগ করা হয়নি।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(uiState.dayExpenses, key = { "exp_${it.id}" }) { exp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, dokanColors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exp.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (exp.note.isNotBlank()) {
                                Text(
                                    text = exp.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = Formatters.formatDateTimeDisplay(exp.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = Formatters.formatCurrency(exp.amount, sym),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = dokanColors.expenseAmber
                        )
                        IconButton(
                            onClick = { deletingExpense = exp },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "খরচ মুছুন",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(uiState.selectedDateKey) {
            Formatters.parseDateKeyToMillis(uiState.selectedDateKey) + 12 * 60 * 60 * 1000L
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onSelectDate(Formatters.dateKeyFromMillis(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("নির্বাচন করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("বাতিল") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDailyBundleDialog) {
        DailySalesBundleDialog(
            dateKey = uiState.selectedDateKey,
            customers = uiState.customers,
            currencySymbol = sym,
            onDismiss = { showDailyBundleDialog = false },
            onSubmit = { cash, credit, cid, expTitle, expAmt, note ->
                showDailyBundleDialog = false
                onRecordDailyBundle(cash, credit, cid, expTitle, expAmt, note)
            }
        )
    }

    if (showExpenseDialog) {
        ExpenseDialog(
            currencySymbol = sym,
            onDismiss = { showExpenseDialog = false },
            onSubmit = { title, amt, note ->
                showExpenseDialog = false
                onAddExpense(title, amt, note)
            }
        )
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
            message = "${tx.type.labelBn} (${Formatters.formatCurrency(tx.amount, sym)}) মুছে ফেললে হিসাব স্বয়ংক্রিয়ভাবে হালনাগাদ হবে।",
            onConfirm = {
                val id = tx.id
                deletingTx = null
                onDeleteTransaction(id)
            },
            onDismiss = { deletingTx = null }
        )
    }

    deletingExpense?.let { exp ->
        ConfirmDeleteDialog(
            title = "খরচ মুছে ফেলবেন?",
            message = "\"${exp.title}\" (${Formatters.formatCurrency(exp.amount, sym)}) খরচটি মুছে ফেলবেন?",
            onConfirm = {
                val id = exp.id
                deletingExpense = null
                onDeleteExpense(id)
            },
            onDismiss = { deletingExpense = null }
        )
    }
}
