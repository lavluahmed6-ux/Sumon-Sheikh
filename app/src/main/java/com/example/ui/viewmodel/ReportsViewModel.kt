package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ShopSettings
import com.example.data.local.TransactionType
import com.example.data.report.PdfReportExporter
import com.example.data.report.ReportSummaryData
import com.example.data.repository.DokanRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReportPeriod(val labelBn: String, val fullTitleBn: String) {
    DAILY("দৈনিক", "দৈনিক ব্যবসার রিপোর্ট"),
    WEEKLY("সাপ্তাহিক", "সাপ্তাহিক ব্যবসার রিপোর্ট (৭ দিন)"),
    MONTHLY("মাসিক", "মাসিক ব্যবসার রিপোর্ট (চলতি মাস)")
}

data class DailyChartPoint(
    val dateKey: String,
    val label: String,
    val totalSales: Double,
    val cashCollected: Double,
    val expenses: Double
)

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.WEEKLY,
    val settings: ShopSettings = ShopSettings(),
    val dailySalesTotal: Double = 0.0,
    val weeklySalesTotal: Double = 0.0,
    val monthlySalesTotal: Double = 0.0,
    val activeReport: ReportSummaryData = ReportSummaryData(
        periodTitle = ReportPeriod.WEEKLY.fullTitleBn,
        totalSales = 0.0,
        cashSales = 0.0,
        creditSales = 0.0,
        paymentCollected = 0.0,
        totalExpenses = 0.0,
        netCashBalance = 0.0,
        totalStoreDue = 0.0,
        totalCustomers = 0,
        customersWithDue = emptyList(),
        transactions = emptyList(),
        expenses = emptyList()
    ),
    val last7DaysChart: List<DailyChartPoint> = emptyList()
)

class ReportsViewModel(
    private val repository: DokanRepository,
    private val pdfExporter: PdfReportExporter
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(ReportPeriod.WEEKLY)

    val uiState: StateFlow<ReportsUiState> = combine(
        selectedPeriod,
        repository.customersFlow,
        repository.transactionsFlow,
        repository.expensesFlow,
        repository.settingsFlow
    ) { period, customers, allTxs, allExpenses, settings ->
        val today = Formatters.todayDateKey()
        val weekStart = Formatters.daysAgoDateKey(6)
        val monthStart = Formatters.currentMonthStartKey()

        val dailySales = allTxs.filter {
            it.dateKey == today && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.CREDIT_SALE)
        }.sumOf { it.amount }

        val weeklySales = allTxs.filter {
            it.dateKey in weekStart..today && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.CREDIT_SALE)
        }.sumOf { it.amount }

        val monthlySales = allTxs.filter {
            it.dateKey in monthStart..today && (it.type == TransactionType.CASH_SALE || it.type == TransactionType.CREDIT_SALE)
        }.sumOf { it.amount }

        val periodTxs = when (period) {
            ReportPeriod.DAILY -> allTxs.filter { it.dateKey == today }
            ReportPeriod.WEEKLY -> allTxs.filter { it.dateKey in weekStart..today }
            ReportPeriod.MONTHLY -> allTxs.filter { it.dateKey in monthStart..today }
        }
        val periodExpenses = when (period) {
            ReportPeriod.DAILY -> allExpenses.filter { it.dateKey == today }
            ReportPeriod.WEEKLY -> allExpenses.filter { it.dateKey in weekStart..today }
            ReportPeriod.MONTHLY -> allExpenses.filter { it.dateKey in monthStart..today }
        }

        val cashSales = periodTxs.filter { it.type == TransactionType.CASH_SALE }.sumOf { it.amount }
        val creditSales = periodTxs.filter { it.type == TransactionType.CREDIT_SALE }.sumOf { it.amount }
        val paymentCollected = periodTxs.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }
        val expenseTotal = periodExpenses.sumOf { it.amount }
        val netCash = (cashSales + paymentCollected) - expenseTotal
        val dueCustomers = customers.filter { it.currentDue > 0.0 }.sortedByDescending { it.currentDue }

        val chartPoints = (6 downTo 0).map { offset ->
            val key = Formatters.daysAgoDateKey(offset)
            val txs = allTxs.filter { it.dateKey == key }
            val exps = allExpenses.filter { it.dateKey == key }
            val daySales = txs.filter { it.type == TransactionType.CASH_SALE || it.type == TransactionType.CREDIT_SALE }
                .sumOf { it.amount }
            val dayCollected = txs.filter { it.type == TransactionType.CASH_SALE || it.type == TransactionType.PAYMENT }
                .sumOf { it.amount }
            val dayExp = exps.sumOf { it.amount }
            DailyChartPoint(
                dateKey = key,
                label = Formatters.formatShortDayLabel(key),
                totalSales = daySales,
                cashCollected = dayCollected,
                expenses = dayExp
            )
        }

        ReportsUiState(
            period = period,
            settings = settings,
            dailySalesTotal = dailySales,
            weeklySalesTotal = weeklySales,
            monthlySalesTotal = monthlySales,
            activeReport = ReportSummaryData(
                periodTitle = period.fullTitleBn,
                totalSales = cashSales + creditSales,
                cashSales = cashSales,
                creditSales = creditSales,
                paymentCollected = paymentCollected,
                totalExpenses = expenseTotal,
                netCashBalance = netCash,
                totalStoreDue = customers.sumOf { it.currentDue },
                totalCustomers = customers.size,
                customersWithDue = dueCustomers,
                transactions = periodTxs,
                expenses = periodExpenses
            ),
            last7DaysChart = chartPoints
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    fun setPeriod(period: ReportPeriod) {
        selectedPeriod.value = period
    }

    fun exportPdfToUri(uri: Uri, onResult: (Result<Unit>) -> Unit) {
        val state = uiState.value
        viewModelScope.launch {
            onResult(pdfExporter.exportToUri(uri, state.settings, state.activeReport))
        }
    }

    fun createShareablePdfUri(onResult: (Result<Uri>) -> Unit) {
        val state = uiState.value
        viewModelScope.launch {
            onResult(pdfExporter.createShareablePdfUri(state.settings, state.activeReport))
        }
    }
}
