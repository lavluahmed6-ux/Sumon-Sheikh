package com.example.data.backup

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CustomerBackupDto(
    val id: Long,
    val name: String,
    val phone: String,
    val address: String,
    val previousDue: Double,
    val totalPurchase: Double,
    val totalPaid: Double,
    val currentDue: Double,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class TransactionBackupDto(
    val id: Long,
    val customerId: Long?,
    val customerName: String,
    val type: String,
    val amount: Double,
    val note: String,
    val timestamp: Long,
    val dateKey: String
)

@JsonClass(generateAdapter = true)
data class ExpenseBackupDto(
    val id: Long,
    val title: String,
    val amount: Double,
    val note: String,
    val timestamp: Long,
    val dateKey: String
)

@JsonClass(generateAdapter = true)
data class ShopSettingsBackupDto(
    val shopName: String,
    val shopPhone: String,
    val shopAddress: String,
    val currencySymbol: String,
    val isDarkMode: Boolean
)

@JsonClass(generateAdapter = true)
data class DokanBackupPayload(
    val schemaVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val settings: ShopSettingsBackupDto,
    val customers: List<CustomerBackupDto>,
    val transactions: List<TransactionBackupDto>,
    val expenses: List<ExpenseBackupDto>
)
