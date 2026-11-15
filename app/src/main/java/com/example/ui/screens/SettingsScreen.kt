package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.backup.DokanBackupPayload
import com.example.ui.theme.DokanTheme
import com.example.ui.viewmodel.SettingsUiState
import com.example.util.Formatters

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdateShopProfile: (name: String, phone: String, address: String) -> Unit,
    onUpdateCurrency: (String) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onCreateLocalBackup: () -> Unit,
    onExportBackupToUri: (Uri) -> Unit,
    onSelectRestoreFileUri: (Uri) -> Unit,
    onRequestLocalRestore: () -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissRestorePreview: () -> Unit,
    onNavigateReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dokanColors = DokanTheme.colors
    val settings = uiState.settings
    var showEditShopDialog by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupToUri(uri)
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onSelectRestoreFileUri(uri)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "সেটিংস ও ব্যাকআপ",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Shop Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, dokanColors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = dokanColors.ledgerTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("দোকানের তথ্য", style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(
                            onClick = { showEditShopDialog = true },
                            modifier = Modifier.testTag("edit_shop_profile_btn")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "সম্পাদনা করুন")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = settings.shopName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (settings.shopPhone.isNotBlank()) "ফোন: ${settings.shopPhone}" else "ফোন নম্বর যোগ করা হয়নি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (settings.shopAddress.isNotBlank()) "ঠিকানা: ${settings.shopAddress}" else "ঠিকানা যোগ করা হয়নি",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Currency & Theme Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, dokanColors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("কারেন্সি ও থিম", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "মুদ্রা (Currency Symbol)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("৳", "BDT ", "$", "₹").forEach { sym ->
                            FilterChip(
                                selected = settings.currencySymbol == sym,
                                onClick = { onUpdateCurrency(sym) },
                                label = { Text(sym.trim()) },
                                modifier = Modifier.testTag("currency_chip_${sym.trim()}")
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("ডার্ক মোড (Dark Mode)", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "রাতের ব্যবহারে চোখের আরামদায়ক থিম",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = onToggleDarkMode,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }
            }
        }

        // Reports Shortcut
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateReports,
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = dokanColors.salesBlueContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Assessment,
                            contentDescription = null,
                            tint = dokanColors.salesBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "ব্যবসার রিপোর্ট ও PDF এক্সপোর্ট",
                                style = MaterialTheme.typography.titleSmall,
                                color = dokanColors.salesBlue
                            )
                            Text(
                                "দৈনিক, সাপ্তাহিক, মাসিক ও বাকি রিপোর্ট দেখুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = dokanColors.salesBlue.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Data Backup & Restore Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, dokanColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Backup, contentDescription = null, tint = dokanColors.paymentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ডেটা ব্যাকআপ ও রিস্টোর", style = MaterialTheme.typography.titleMedium)
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "বর্তমান ডেটা: ${uiState.customerCount} কাস্টমার • ${uiState.transactionCount} লেনদেন • ${uiState.expenseCount} খরচ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            val backupTimeText = if (settings.lastBackupTimestamp > 0L) {
                                "সর্বশেষ ব্যাকআপ: ${Formatters.formatDateTimeDisplay(settings.lastBackupTimestamp)}"
                            } else {
                                "এখনো কোনো ব্যাকআপ তৈরি করা হয়নি"
                            }
                            Text(
                                text = backupTimeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onCreateLocalBackup,
                        enabled = !uiState.isProcessingBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = dokanColors.paymentGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("local_backup_button")
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ডেটা ব্যাকআপ নিন (ডিভাইসে সংরক্ষণ)")
                    }

                    OutlinedButton(
                        onClick = {
                            val name = "Dokan_Hisab_Backup_${Formatters.todayDateKey()}.json"
                            exportBackupLauncher.launch(name)
                        },
                        enabled = !uiState.isProcessingBackup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_backup_file_button")
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ব্যাকআপ ফাইল এক্সপোর্ট করুন (JSON)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            enabled = !uiState.isProcessingBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_from_file_button")
                        ) {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("ফাইল রিস্টোর", style = MaterialTheme.typography.labelMedium)
                        }

                        if (uiState.hasLocalBackup) {
                            OutlinedButton(
                                onClick = onRequestLocalRestore,
                                enabled = !uiState.isProcessingBackup,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_local_backup_button")
                            ) {
                                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("লোকাল রিস্টোর", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // App Information Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, dokanColors.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = dokanColors.ledgerTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("স্মার্ট দোকান হিসাব (Smart Dokan Hisab)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ভার্সন ১.০ • সম্পূর্ণ অফলাইন Room SQLite ডাটাবেজ দ্বারা পরিচালিত। আপনার দোকানের সব তথ্য নিরাপদে আপনার ফোনেই সংরক্ষিত থাকে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showEditShopDialog) {
        EditShopProfileDialog(
            initialName = settings.shopName,
            initialPhone = settings.shopPhone,
            initialAddress = settings.shopAddress,
            onDismiss = { showEditShopDialog = false },
            onSave = { name, phone, address ->
                showEditShopDialog = false
                onUpdateShopProfile(name, phone, address)
            }
        )
    }

    uiState.pendingRestorePayload?.let { payload ->
        RestoreConfirmationDialog(
            payload = payload,
            onConfirm = onConfirmRestore,
            onDismiss = onDismissRestorePreview
        )
    }
}

@Composable
private fun EditShopProfileDialog(
    initialName: String,
    initialPhone: String,
    initialAddress: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf(initialAddress) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("দোকানের তথ্য পরিবর্তন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("দোকানের নাম") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ফোন নম্বর") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("দোকানের ঠিকানা") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_address_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, address) },
                modifier = Modifier.testTag("save_shop_profile_btn")
            ) { Text("সংরক্ষণ করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun RestoreConfirmationDialog(
    payload: DokanBackupPayload,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ব্যাকআপ রিস্টোর করবেন?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "রিস্টোর করলে বর্তমান ডেটার পরিবর্তে নিচের ব্যাকআপ ডেটা লোড হবে:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("• দোকান: ${payload.settings.shopName}", style = MaterialTheme.typography.bodySmall)
                Text("• কাস্টমার: ${payload.customers.size} জন", style = MaterialTheme.typography.bodySmall)
                Text("• লেনদেন: ${payload.transactions.size} টি", style = MaterialTheme.typography.bodySmall)
                Text("• খরচ: ${payload.expenses.size} টি", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "• ব্যাকআপের সময়: ${Formatters.formatDateTimeDisplay(payload.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_restore_btn")
            ) { Text("হ্যাঁ, রিস্টোর করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
