package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.DokanTheme
import com.example.ui.viewmodel.CustomersUiState
import com.example.ui.viewmodel.DueSortOption
import com.example.util.Formatters

@Composable
fun DueScreen(
    uiState: CustomersUiState,
    onSearchChange: (String) -> Unit,
    onSortChange: (DueSortOption) -> Unit,
    onCustomerClick: (Long) -> Unit,
    onReceivePaymentFor: (CustomerEntity) -> Unit,
    onAddCreditFor: (CustomerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dokanColors = DokanTheme.colors
    val sym = uiState.settings.currencySymbol

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Total Due Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(dokanColors.dueRed, dokanColors.dueOrange)
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "দোকানের মোট বাকি",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(uiState.totalOutstandingDue, sym),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.testTag("total_store_due_amount")
                        )
                        Text(
                            text = "বাকি থাকা কাস্টমার: ${uiState.dueCustomers.size} জন",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.dueSearchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("বাকি কাস্টমার খুঁজুন (নাম বা ফোন)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.dueSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "পরিষ্কার করুন")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("due_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DueSortOption.entries) { option ->
                FilterChip(
                    selected = uiState.dueSortOption == option,
                    onClick = { onSortChange(option) },
                    label = { Text(option.labelBn) },
                    modifier = Modifier.testTag("due_sort_${option.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.dueCustomers.isEmpty()) {
            EmptyStateView(
                title = "কোনো কাস্টমারের বাকি নেই",
                subtitle = "সব কাস্টমারের হিসাব পরিশোধিত আছে অথবা খুঁজতে অন্য নাম লিখুন।",
                icon = Icons.Outlined.CheckCircle
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.dueCustomers, key = { it.id }) { customer ->
                    DueCustomerCard(
                        customer = customer,
                        currencySymbol = sym,
                        onClick = { onCustomerClick(customer.id) },
                        onReceivePayment = { onReceivePaymentFor(customer) },
                        onAddCredit = { onAddCreditFor(customer) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DueCustomerCard(
    customer: CustomerEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    onReceivePayment: () -> Unit,
    onAddCredit: () -> Unit
) {
    val dokanColors = DokanTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, dokanColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = dokanColors.dueRedContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = Formatters.formatCurrency(customer.currentDue, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = dokanColors.dueRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReceivePayment,
                    colors = ButtonDefaults.buttonColors(containerColor = dokanColors.paymentGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("due_pay_${customer.id}")
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("টাকা জমা", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onAddCredit,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("due_add_credit_${customer.id}")
                ) {
                    Icon(
                        Icons.Outlined.AddShoppingCart,
                        contentDescription = null,
                        tint = dokanColors.dueRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("বাকি যোগ", style = MaterialTheme.typography.labelMedium, color = dokanColors.dueRed)
                }
            }
        }
    }
}
