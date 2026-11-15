package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SummaryMetricCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.DokanTheme
import com.example.ui.viewmodel.HomeUiState
import com.example.util.Formatters

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenNewCustomer: () -> Unit,
    onOpenCashSale: () -> Unit,
    onOpenCreditSale: () -> Unit,
    onOpenReceivePayment: () -> Unit,
    onNavigateCustomers: () -> Unit,
    onNavigateSales: () -> Unit,
    onNavigateDue: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateReports: () -> Unit,
    onOpenCustomerDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dokanColors = DokanTheme.colors
    val sym = uiState.settings.currencySymbol
    var showNewSaleChoice by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Shop Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = dokanColors.ledgerTeal)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_shop_hero),
                        contentDescription = "দোকান হিসাব ড্যাশবোর্ড",
                        contentScale = ContentScale.Crop,
                        alpha = 0.28f,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0A4B5D).copy(alpha = 0.94f),
                                        Color(0xFF1565C0).copy(alpha = 0.80f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.settings.shopName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "অফলাইন নিরাপদ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = Formatters.formatDateBn(Formatters.todayDateKey()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "আজকের মোট বিক্রি: ${Formatters.formatCurrency(uiState.todayTotalSales, sym)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 6 Summary Cards Grid (2 columns x 3 rows)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = "আজকের মোট বিক্রি",
                        value = Formatters.formatCurrency(uiState.todayTotalSales, sym),
                        icon = Icons.Outlined.PointOfSale,
                        accentColor = dokanColors.salesBlue,
                        containerColor = dokanColors.salesBlueContainer,
                        onClick = onNavigateSales,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_today_sales")
                    )
                    SummaryMetricCard(
                        title = "আজকে মোট টাকা জমা",
                        value = Formatters.formatCurrency(uiState.todayCashReceived, sym),
                        icon = Icons.Outlined.Payments,
                        accentColor = dokanColors.paymentGreen,
                        containerColor = dokanColors.paymentGreenContainer,
                        onClick = onNavigateSales,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_today_collection")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = "আজকে মোট বাকি বিক্রি",
                        value = Formatters.formatCurrency(uiState.todayCreditSales, sym),
                        icon = Icons.Outlined.AddShoppingCart,
                        accentColor = dokanColors.dueOrange,
                        containerColor = dokanColors.dueOrangeContainer,
                        onClick = onNavigateDue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_today_credit")
                    )
                    SummaryMetricCard(
                        title = "দোকানের মোট বাকি",
                        value = Formatters.formatCurrency(uiState.totalStoreDue, sym),
                        icon = Icons.Outlined.WarningAmber,
                        accentColor = dokanColors.dueRed,
                        containerColor = dokanColors.dueRedContainer,
                        onClick = onNavigateDue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_due")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = "মোট কাস্টমার",
                        value = "${uiState.totalCustomers} জন",
                        icon = Icons.Outlined.Group,
                        accentColor = dokanColors.ledgerTeal,
                        containerColor = dokanColors.ledgerTealContainer,
                        onClick = onNavigateCustomers,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_customers")
                    )
                    SummaryMetricCard(
                        title = "আজকের মোট লেনদেন",
                        value = "${uiState.todayTransactionCount} টি",
                        icon = Icons.Outlined.ReceiptLong,
                        accentColor = dokanColors.salesBlue,
                        containerColor = dokanColors.salesBlueContainer,
                        onClick = onNavigateHistory,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_today_tx_count")
                    )
                }
            }
        }

        // Quick Actions Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "দ্রুত কার্যক্রম",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val quickActions = listOf(
                    QuickActionItem(
                        label = "নতুন কাস্টমার",
                        icon = Icons.Outlined.PersonAdd,
                        accent = dokanColors.ledgerTeal,
                        container = dokanColors.ledgerTealContainer,
                        tag = "qa_new_customer",
                        onClick = onOpenNewCustomer
                    ),
                    QuickActionItem(
                        label = "নতুন বিক্রি",
                        icon = Icons.Outlined.PointOfSale,
                        accent = dokanColors.salesBlue,
                        container = dokanColors.salesBlueContainer,
                        tag = "qa_new_sale",
                        onClick = { showNewSaleChoice = true }
                    ),
                    QuickActionItem(
                        label = "টাকা জমা",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        accent = dokanColors.paymentGreen,
                        container = dokanColors.paymentGreenContainer,
                        tag = "qa_receive_payment",
                        onClick = onOpenReceivePayment
                    ),
                    QuickActionItem(
                        label = "কাস্টমার তালিকা",
                        icon = Icons.Outlined.Group,
                        accent = dokanColors.salesBlue,
                        container = dokanColors.salesBlueContainer,
                        tag = "qa_customer_list",
                        onClick = onNavigateCustomers
                    ),
                    QuickActionItem(
                        label = "দৈনিক হিসাব",
                        icon = Icons.Outlined.ReceiptLong,
                        accent = dokanColors.dueOrange,
                        container = dokanColors.dueOrangeContainer,
                        tag = "qa_daily_accounts",
                        onClick = onNavigateSales
                    ),
                    QuickActionItem(
                        label = "রিপোর্ট ও PDF",
                        icon = Icons.Outlined.Assessment,
                        accent = dokanColors.ledgerTeal,
                        container = dokanColors.ledgerTealContainer,
                        tag = "qa_reports",
                        onClick = onNavigateReports
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quickActions, key = { it.tag }) { action ->
                        Card(
                            modifier = Modifier
                                .width(116.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(onClick = action.onClick)
                                .testTag(action.tag),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, dokanColors.cardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(action.container),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = action.label,
                                        tint = action.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "সাম্প্রতিক লেনদেন",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                TextButton(
                    onClick = onNavigateHistory,
                    modifier = Modifier.testTag("view_all_history_button")
                ) {
                    Text("সব দেখুন")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (uiState.recentTransactions.isEmpty()) {
            item {
                EmptyStateView(
                    title = "এখনো কোনো লেনদেন হয়নি",
                    subtitle = "উপরের 'নতুন বিক্রি' বা 'নতুন কাস্টমার' বাটনে চাপ দিয়ে আজকের হিসাব শুরু করুন।"
                )
            }
        } else {
            items(uiState.recentTransactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    currencySymbol = sym,
                    onClick = tx.customerId?.let { cid -> { onOpenCustomerDetail(cid) } }
                )
            }
        }
    }

    if (showNewSaleChoice) {
        AlertDialog(
            onDismissRequest = { showNewSaleChoice = false },
            title = { Text("বিক্রির ধরন বেছে নিন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showNewSaleChoice = false
                            onOpenCashSale()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dokanColors.salesBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("choice_cash_sale")
                    ) {
                        Text("নগদ বিক্রি (Cash Sale)")
                    }
                    Button(
                        onClick = {
                            showNewSaleChoice = false
                            onOpenCreditSale()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dokanColors.dueRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("choice_credit_sale")
                    ) {
                        Text("বাকি বিক্রি (Credit Sale)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNewSaleChoice = false }) { Text("বাতিল") }
            }
        )
    }
}

private data class QuickActionItem(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val container: Color,
    val tag: String,
    val onClick: () -> Unit
)
