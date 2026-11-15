package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.BreakdownDonutCard
import com.example.ui.components.SevenDayBarChartCard
import com.example.ui.components.SummaryMetricCard
import com.example.ui.theme.DokanTheme
import com.example.ui.viewmodel.ReportPeriod
import com.example.ui.viewmodel.ReportsUiState
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onPeriodChange: (ReportPeriod) -> Unit,
    onExportPdfToUri: (android.net.Uri) -> Unit,
    onSharePdf: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val dokanColors = DokanTheme.colors
    val sym = uiState.settings.currencySymbol
    val report = uiState.activeReport

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            onExportPdfToUri(uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("রিপোর্ট ও বিশ্লেষণ") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily / Weekly / Monthly Sales Overview Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodOverviewPill(
                        label = "দৈনিক বিক্রি",
                        value = Formatters.formatCurrency(uiState.dailySalesTotal, sym),
                        selected = uiState.period == ReportPeriod.DAILY,
                        onClick = { onPeriodChange(ReportPeriod.DAILY) },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodOverviewPill(
                        label = "সাপ্তাহিক বিক্রি",
                        value = Formatters.formatCurrency(uiState.weeklySalesTotal, sym),
                        selected = uiState.period == ReportPeriod.WEEKLY,
                        onClick = { onPeriodChange(ReportPeriod.WEEKLY) },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodOverviewPill(
                        label = "মাসিক বিক্রি",
                        value = Formatters.formatCurrency(uiState.monthlySalesTotal, sym),
                        selected = uiState.period == ReportPeriod.MONTHLY,
                        onClick = { onPeriodChange(ReportPeriod.MONTHLY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter Chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = uiState.period == p,
                            onClick = { onPeriodChange(p) },
                            label = { Text(p.labelBn) },
                            modifier = Modifier.testTag("report_period_${p.name}")
                        )
                    }
                }
            }

            // PDF Export & Share Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val fileName = "Dokan_Hisab_Report_${Formatters.todayDateKey()}.pdf"
                            createPdfLauncher.launch(fileName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dokanColors.ledgerTeal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF সেভ করুন")
                    }
                    OutlinedButton(
                        onClick = onSharePdf,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_pdf_button")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শেয়ার করুন")
                    }
                }
            }

            // 4 Core Period Report Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMetricCard(
                            title = "${uiState.period.labelBn} মোট বিক্রি",
                            value = Formatters.formatCurrency(report.totalSales, sym),
                            subtitle = "নগদ+বাকি",
                            icon = Icons.Outlined.PointOfSale,
                            accentColor = dokanColors.salesBlue,
                            containerColor = dokanColors.salesBlueContainer,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            title = "মোট টাকা আদায়",
                            value = Formatters.formatCurrency(report.paymentCollected, sym),
                            subtitle = "বাকি পরিশোধ",
                            icon = Icons.Outlined.AccountBalanceWallet,
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
                            title = "মোট কাস্টমার বাকি",
                            value = Formatters.formatCurrency(report.totalStoreDue, sym),
                            subtitle = "${report.customersWithDue.size} জন",
                            icon = Icons.Outlined.WarningAmber,
                            accentColor = dokanColors.dueRed,
                            containerColor = dokanColors.dueRedContainer,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            title = "${uiState.period.labelBn} মোট খরচ",
                            value = Formatters.formatCurrency(report.totalExpenses, sym),
                            subtitle = "${report.expenses.size} টি",
                            icon = Icons.Outlined.MoneyOff,
                            accentColor = dokanColors.expenseAmber,
                            containerColor = dokanColors.expenseContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 7-Day Bar Chart
            item {
                SevenDayBarChartCard(
                    points = uiState.last7DaysChart,
                    currencySymbol = sym
                )
            }

            // Donut Breakdown
            item {
                BreakdownDonutCard(
                    cashSales = report.cashSales,
                    creditSales = report.creditSales,
                    paymentCollected = report.paymentCollected,
                    currencySymbol = sym
                )
            }

            // Top Customer Due Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, dokanColors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "শীর্ষ বাকি কাস্টমার রিপোর্ট",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (report.customersWithDue.isEmpty()) {
                            Text(
                                text = "কোনো কাস্টমারের কাছে বাকি পাওনা নেই।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            report.customersWithDue.take(8).forEachIndexed { index, c ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${index + 1}. ${c.name}", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            c.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = Formatters.formatCurrency(c.currentDue, sym),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = dokanColors.dueRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodOverviewPill(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else DokanTheme.colors.cardBorder
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
