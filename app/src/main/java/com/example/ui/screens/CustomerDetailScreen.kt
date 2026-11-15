package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.data.local.CustomerEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.CashSaleDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreditSaleDialog
import com.example.ui.components.CustomerFormDialog
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ReceivePaymentDialog
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.DokanTheme
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: CustomerEntity?,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    onBack: () -> Unit,
    onUpdateCustomer: (name: String, phone: String, address: String, prevDue: Double) -> Unit,
    onDeleteCustomer: () -> Unit,
    onCreditSale: (totalPurchase: Double, paidNow: Double, note: String) -> Unit,
    onReceivePayment: (amount: Double, note: String) -> Unit,
    onCashSale: (amount: Double, note: String) -> Unit,
    onUpdateTransaction: (TransactionEntity, TransactionType, Double, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val dokanColors = DokanTheme.colors
    var showEditCustomer by remember { mutableStateOf(false) }
    var showDeleteCustomer by remember { mutableStateOf(false) }
    var showCreditDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showCashSaleDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var deletingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "কাস্টমার বিবরণ") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("customer_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    if (customer != null) {
                        IconButton(
                            onClick = { showEditCustomer = true },
                            modifier = Modifier.testTag("edit_customer_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা করুন")
                        }
                        IconButton(
                            onClick = { showDeleteCustomer = true },
                            modifier = Modifier.testTag("delete_customer_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "মুছে ফেলুন",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (customer == null) {
            EmptyStateView(
                title = "কাস্টমার পাওয়া যায়নি",
                subtitle = "এই কাস্টমারের তথ্য মুছে ফেলা হয়ে থাকতে পারে।",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile & Balance Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, dokanColors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = customer.phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (customer.address.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = customer.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            val hasDue = customer.currentDue > 0.0
                            Surface(
                                color = if (hasDue) dokanColors.dueRedContainer else dokanColors.paymentGreenContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = if (hasDue) "বর্তমান বাকি" else "হিসাব পরিশোধিত",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasDue) dokanColors.dueRed else dokanColors.paymentGreen
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(customer.currentDue, currencySymbol),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasDue) dokanColors.dueRed else dokanColors.paymentGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniStatPill(
                                label = "আগের বাকি",
                                value = Formatters.formatCurrency(customer.previousDue, currencySymbol),
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatPill(
                                label = "মোট কেনাকাটা",
                                value = Formatters.formatCurrency(customer.totalPurchase, currencySymbol),
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatPill(
                                label = "মোট জমা",
                                value = Formatters.formatCurrency(customer.totalPaid, currencySymbol),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showCreditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = dokanColors.dueRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_credit_sale_button")
                    ) {
                        Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("বাকি বিক্রি")
                    }
                    Button(
                        onClick = { showPaymentDialog = true },
                        enabled = customer.currentDue > 0.0,
                        colors = ButtonDefaults.buttonColors(containerColor = dokanColors.paymentGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_payment_button")
                    ) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("টাকা জমা")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showCashSaleDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_cash_sale_button")
                ) {
                    Icon(Icons.Outlined.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("নগদ বিক্রি যোগ করুন")
                }
            }

            item {
                Text(
                    text = "লেনদেনের ইতিহাস (${transactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "কোনো লেনদেন নেই",
                        subtitle = "এই কাস্টমারের সাথে নতুন বিক্রি বা টাকা জমা যোগ করুন।"
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        currencySymbol = currencySymbol,
                        showActions = true,
                        onEdit = { editingTx = tx },
                        onDelete = { deletingTx = tx }
                    )
                }
            }
        }
    }

    if (showEditCustomer && customer != null) {
        CustomerFormDialog(
            initialCustomer = customer,
            currencySymbol = currencySymbol,
            onDismiss = { showEditCustomer = false },
            onSubmit = { name, phone, address, prevDue ->
                showEditCustomer = false
                onUpdateCustomer(name, phone, address, prevDue)
            }
        )
    }

    if (showDeleteCustomer && customer != null) {
        ConfirmDeleteDialog(
            title = "কাস্টমার মুছে ফেলবেন?",
            message = "\"${customer.name}\" এবং তাঁর সব লেনদেনের রেকর্ড স্থায়ীভাবে মুছে যাবে। আপনি কি নিশ্চিত?",
            onConfirm = {
                showDeleteCustomer = false
                onDeleteCustomer()
            },
            onDismiss = { showDeleteCustomer = false }
        )
    }

    if (showCreditDialog && customer != null) {
        CreditSaleDialog(
            customers = listOf(customer),
            preselectedCustomer = customer,
            currencySymbol = currencySymbol,
            onDismiss = { showCreditDialog = false },
            onSubmit = { _, purchase, paidNow, note ->
                showCreditDialog = false
                onCreditSale(purchase, paidNow, note)
            }
        )
    }

    if (showPaymentDialog && customer != null) {
        ReceivePaymentDialog(
            customers = listOf(customer),
            preselectedCustomer = customer,
            currencySymbol = currencySymbol,
            onDismiss = { showPaymentDialog = false },
            onSubmit = { _, amt, note ->
                showPaymentDialog = false
                onReceivePayment(amt, note)
            }
        )
    }

    if (showCashSaleDialog && customer != null) {
        CashSaleDialog(
            customers = listOf(customer),
            preselectedCustomer = customer,
            currencySymbol = currencySymbol,
            onDismiss = { showCashSaleDialog = false },
            onSubmit = { _, amt, note ->
                showCashSaleDialog = false
                onCashSale(amt, note)
            }
        )
    }

    editingTx?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            currencySymbol = currencySymbol,
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
            message = "${tx.type.labelBn} (${Formatters.formatCurrency(tx.amount, currencySymbol)}) মুছে ফেললে কাস্টমারের বাকি স্বয়ংক্রিয়ভাবে হালনাগাদ হবে।",
            onConfirm = {
                val id = tx.id
                deletingTx = null
                onDeleteTransaction(id)
            },
            onDismiss = { deletingTx = null }
        )
    }
}

@Composable
private fun MiniStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
