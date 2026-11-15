package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.theme.DokanTheme
import com.example.util.Formatters

@Composable
fun CustomerFormDialog(
    initialCustomer: CustomerEntity? = null,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, address: String, previousDue: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var previousDueText by remember {
        mutableStateOf(
            if (initialCustomer != null && initialCustomer.previousDue > 0.0) {
                Formatters.formatAmountPlain(initialCustomer.previousDue)
            } else ""
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCustomer == null) "নতুন কাস্টমার যোগ করুন" else "কাস্টমারের তথ্য সম্পাদনা",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("কাস্টমারের নাম *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    label = { Text("ফোন নম্বর *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input")
                )
                OutlinedTextField(
                    value = previousDueText,
                    onValueChange = { previousDueText = filterDecimalInput(it); errorMsg = null },
                    label = { Text("আগের বাকি ($currencySymbol - ঐচ্ছিক)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_prev_due_input")
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prevDue = previousDueText.toDoubleOrNull() ?: 0.0
                    when {
                        name.isBlank() -> errorMsg = "কাস্টমারের নাম লিখুন"
                        phone.isBlank() -> errorMsg = "ফোন নম্বর লিখুন"
                        prevDue < 0.0 -> errorMsg = "আগের বাকি ঋণাত্মক হতে পারবে না"
                        else -> onSubmit(name, phone, address, prevDue)
                    }
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(if (initialCustomer == null) "সংরক্ষণ করুন" else "আপডেট করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun CreditSaleDialog(
    customers: List<CustomerEntity>,
    preselectedCustomer: CustomerEntity? = null,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSubmit: (customerId: Long, totalPurchase: Double, paidNow: Double, note: String) -> Unit
) {
    val dokanColors = DokanTheme.colors
    var selectedCustomer by remember { mutableStateOf(preselectedCustomer ?: customers.firstOrNull()) }
    var purchaseText by remember { mutableStateOf("") }
    var paidNowText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val purchaseVal = purchaseText.toDoubleOrNull() ?: 0.0
    val paidVal = paidNowText.toDoubleOrNull() ?: 0.0
    val remainingDue = (purchaseVal - paidVal).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "বাকি বিক্রি যোগ করুন", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (customers.isEmpty()) {
                    Text(
                        text = "প্রথমে অন্তত একজন কাস্টমার যোগ করুন।",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    CustomerSelectorBox(
                        selectedCustomer = selectedCustomer,
                        currencySymbol = currencySymbol,
                        enabled = preselectedCustomer == null,
                        onClick = { showCustomerPicker = true }
                    )

                    OutlinedTextField(
                        value = purchaseText,
                        onValueChange = { purchaseText = filterDecimalInput(it); errorMsg = null },
                        label = { Text("মোট কেনাকাটা / বিলের পরিমাণ ($currencySymbol) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("credit_purchase_amount_input")
                    )

                    OutlinedTextField(
                        value = paidNowText,
                        onValueChange = { paidNowText = filterDecimalInput(it); errorMsg = null },
                        label = { Text("নগদ জমা দিলে লিখুন ($currencySymbol - ঐচ্ছিক)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("credit_paid_now_input")
                    )

                    if (purchaseVal > 0.0) {
                        Surface(
                            color = dokanColors.dueRedContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "অবশিষ্ট বাকি:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = dokanColors.dueRed
                                )
                                Text(
                                    text = Formatters.formatCurrency(remainingDue, currencySymbol),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = dokanColors.dueRed
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("বিবরণ / পণ্যের নাম (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("credit_note_input")
                    )
                }

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = customers.isNotEmpty() && selectedCustomer != null,
                colors = ButtonDefaults.buttonColors(containerColor = dokanColors.dueRed),
                onClick = {
                    val c = selectedCustomer
                    when {
                        c == null -> errorMsg = "কাস্টমার নির্বাচন করুন"
                        purchaseVal <= 0.0 -> errorMsg = "সঠিক পরিমাণ লিখুন"
                        paidVal < 0.0 || paidVal > purchaseVal -> errorMsg = "নগদ জমা মোট বিলের বেশি হতে পারবে না"
                        else -> onSubmit(c.id, purchaseVal, paidVal, note)
                    }
                },
                modifier = Modifier.testTag("save_credit_sale_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            currencySymbol = currencySymbol,
            onSelect = { selectedCustomer = it; showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false }
        )
    }
}

@Composable
fun ReceivePaymentDialog(
    customers: List<CustomerEntity>,
    preselectedCustomer: CustomerEntity? = null,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSubmit: (customerId: Long, amount: Double, note: String) -> Unit
) {
    val dokanColors = DokanTheme.colors
    val eligibleCustomers = remember(customers, preselectedCustomer) {
        if (preselectedCustomer != null) listOf(preselectedCustomer)
        else customers.filter { it.currentDue > 0.0 }.ifEmpty { customers }
    }
    var selectedCustomer by remember { mutableStateOf(preselectedCustomer ?: eligibleCustomers.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "টাকা জমা নিন", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (eligibleCustomers.isEmpty()) {
                    Text("কোনো কাস্টমার পাওয়া যায়নি।", style = MaterialTheme.typography.bodyMedium)
                } else {
                    CustomerSelectorBox(
                        selectedCustomer = selectedCustomer,
                        currencySymbol = currencySymbol,
                        enabled = preselectedCustomer == null,
                        onClick = { showCustomerPicker = true }
                    )

                    selectedCustomer?.let { c ->
                        Surface(
                            color = dokanColors.dueOrangeContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "মোট বাকি:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = dokanColors.dueOrange
                                )
                                Text(
                                    text = Formatters.formatCurrency(c.currentDue, currencySymbol),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = dokanColors.dueOrange
                                )
                            }
                        }
                        if (c.currentDue > 0.0) {
                            TextButton(
                                onClick = {
                                    amountText = Formatters.formatAmountPlain(c.currentDue)
                                    errorMsg = null
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("সম্পূর্ণ বাকি (${Formatters.formatCurrency(c.currentDue, currencySymbol)})")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = filterDecimalInput(it); errorMsg = null },
                        label = { Text("জমার পরিমাণ ($currencySymbol) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_amount_input")
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("জমার নোট (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_note_input")
                    )
                }

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = eligibleCustomers.isNotEmpty() && selectedCustomer != null,
                colors = ButtonDefaults.buttonColors(containerColor = dokanColors.paymentGreen),
                onClick = {
                    val c = selectedCustomer
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    when {
                        c == null -> errorMsg = "কাস্টমার নির্বাচন করুন"
                        amt <= 0.0 -> errorMsg = "জমার পরিমাণ ০-এর বেশি হতে হবে"
                        amt > c.currentDue + 0.001 -> {
                            errorMsg = "বাকির (${Formatters.formatCurrency(c.currentDue, currencySymbol)}) চেয়ে বেশি জমা দেওয়া যাবে না"
                        }
                        else -> onSubmit(c.id, amt, note)
                    }
                },
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text("জমা সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = eligibleCustomers,
            currencySymbol = currencySymbol,
            onSelect = { selectedCustomer = it; showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false }
        )
    }
}

@Composable
fun CashSaleDialog(
    customers: List<CustomerEntity>,
    preselectedCustomer: CustomerEntity? = null,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSubmit: (customerId: Long?, amount: Double, note: String) -> Unit
) {
    val dokanColors = DokanTheme.colors
    var selectedCustomer by remember { mutableStateOf(preselectedCustomer) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নগদ বিক্রি যোগ করুন", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (preselectedCustomer == null && customers.isNotEmpty()) {
                    CustomerSelectorBox(
                        selectedCustomer = selectedCustomer,
                        placeholder = "সাধারণ নগদ ক্রেতা (ঐচ্ছিক)",
                        currencySymbol = currencySymbol,
                        enabled = true,
                        onClick = { showCustomerPicker = true }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = filterDecimalInput(it); errorMsg = null },
                    label = { Text("বিক্রির পরিমাণ ($currencySymbol) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_sale_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("বিবরণ (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_sale_note_input")
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = dokanColors.salesBlue),
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        errorMsg = "বিক্রির পরিমাণ ০-এর বেশি হতে হবে"
                    } else {
                        onSubmit(selectedCustomer?.id, amt, note)
                    }
                },
                modifier = Modifier.testTag("save_cash_sale_button")
            ) {
                Text("বিক্রি সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            allowNoneOption = true,
            currencySymbol = currencySymbol,
            onSelect = { selectedCustomer = it; showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false }
        )
    }
}

@Composable
fun DailySalesBundleDialog(
    dateKey: String,
    customers: List<CustomerEntity>,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSubmit: (
        cashSales: Double,
        creditSales: Double,
        customerId: Long?,
        expenseTitle: String,
        expenseAmount: Double,
        note: String
    ) -> Unit
) {
    val dokanColors = DokanTheme.colors
    var cashText by remember { mutableStateOf("") }
    var creditText by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var expenseTitle by remember { mutableStateOf("") }
    var expenseText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val cashVal = cashText.toDoubleOrNull() ?: 0.0
    val creditVal = creditText.toDoubleOrNull() ?: 0.0
    val expenseVal = expenseText.toDoubleOrNull() ?: 0.0
    val totalSales = cashVal + creditVal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("দৈনিক বিক্রি ও খরচ এন্ট্রি", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = Formatters.formatDateBn(dateKey),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = filterDecimalInput(it); errorMsg = null },
                    label = { Text("নগদ বিক্রি ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_cash_sales_input")
                )
                OutlinedTextField(
                    value = creditText,
                    onValueChange = { creditText = filterDecimalInput(it); errorMsg = null },
                    label = { Text("বাকি বিক্রি ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_credit_sales_input")
                )
                if (creditVal > 0.0 && customers.isNotEmpty()) {
                    CustomerSelectorBox(
                        selectedCustomer = selectedCustomer,
                        placeholder = "কাস্টমার বাছাই (ঐচ্ছিক - বাকি হিসাবের জন্য)",
                        currencySymbol = currencySymbol,
                        enabled = true,
                        onClick = { showCustomerPicker = true }
                    )
                }

                Surface(
                    color = dokanColors.salesBlueContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("মোট বিক্রি (স্বয়ংক্রিয়):", color = dokanColors.salesBlue)
                        Text(
                            text = Formatters.formatCurrency(totalSales, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            color = dokanColors.salesBlue
                        )
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = expenseTitle,
                    onValueChange = { expenseTitle = it },
                    label = { Text("অতিরিক্ত খরচের বিবরণ (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expenseText,
                    onValueChange = { expenseText = filterDecimalInput(it); errorMsg = null },
                    label = { Text("খরচের পরিমাণ ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_expense_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cashVal == 0.0 && creditVal == 0.0 && expenseVal == 0.0) {
                        errorMsg = "নগদ বিক্রি, বাকি বিক্রি অথবা খরচ লিখুন"
                    } else {
                        onSubmit(cashVal, creditVal, selectedCustomer?.id, expenseTitle, expenseVal, note)
                    }
                },
                modifier = Modifier.testTag("save_daily_bundle_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            allowNoneOption = true,
            currencySymbol = currencySymbol,
            onSelect = { selectedCustomer = it; showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false }
        )
    }
}

@Composable
fun ExpenseDialog(
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSubmit: (title: String, amount: Double, note: String) -> Unit
) {
    val dokanColors = DokanTheme.colors
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("দোকান খরচ যোগ করুন", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("খরচের খাত (যেমন: বিদ্যুৎ বিল, ভাড়া) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = filterDecimalInput(it); errorMsg = null },
                    label = { Text("পরিমাণ ($currencySymbol) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = dokanColors.dueOrange),
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    when {
                        title.isBlank() -> errorMsg = "খরচের খাত লিখুন"
                        amt <= 0.0 -> errorMsg = "সঠিক পরিমাণ লিখুন"
                        else -> onSubmit(title, amt, note)
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSubmit: (type: TransactionType, amount: Double, note: String) -> Unit
) {
    var type by remember { mutableStateOf(transaction.type) }
    var amountText by remember { mutableStateOf(Formatters.formatAmountPlain(transaction.amount)) }
    var note by remember { mutableStateOf(transaction.note) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("লেনদেন সম্পাদনা", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "কাস্টমার: ${transaction.customerName}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (transaction.customerId != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TransactionType.entries.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.labelBn, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = filterDecimalInput(it); errorMsg = null },
                    label = { Text("পরিমাণ ($currencySymbol) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("বিবরণ (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        errorMsg = "পরিমাণ ০-এর বেশি হতে হবে"
                    } else {
                        onSubmit(type, amt, note)
                    }
                }
            ) {
                Text("আপডেট করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun CustomerSelectorBox(
    selectedCustomer: CustomerEntity?,
    currencySymbol: String,
    enabled: Boolean,
    placeholder: String = "কাস্টমার নির্বাচন করুন",
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedCustomer?.name ?: placeholder,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedCustomer != null) {
                    Text(
                        text = "${selectedCustomer.phone} • বাকি: ${Formatters.formatCurrency(selectedCustomer.currentDue, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "কাস্টমার পরিবর্তন করুন"
                )
            }
        }
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    allowNoneOption: Boolean = false,
    currencySymbol: String,
    onSelect: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(customers, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) customers
        else customers.filter { it.name.lowercase().contains(q) || it.phone.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("কাস্টমার নির্বাচন করুন") },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("নাম বা ফোন দিয়ে খুঁজুন") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    if (allowNoneOption) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(null) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("সাধারণ ক্রেতা (কাস্টমার ছাড়া)", fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                            HorizontalDivider()
                        }
                    }
                    items(filtered, key = { it.id }) { customer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(customer) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    customer.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (customer.currentDue > 0.0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DokanTheme.colors.dueRedContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = Formatters.formatCurrency(customer.currentDue, currencySymbol),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DokanTheme.colors.dueRed
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
        }
    )
}

private fun filterDecimalInput(input: String): String {
    var dotSeen = false
    val sb = StringBuilder()
    for (ch in input) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && !dotSeen) {
            dotSeen = true
            sb.append(ch)
        }
    }
    return sb.toString()
}
