package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ShopSettings
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.DokanRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class BackupManager(
    private val context: Context,
    private val repository: DokanRepository
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(DokanBackupPayload::class.java).indent("  ")

    private val localBackupFile: File
        get() {
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) dir.mkdirs()
            return File(dir, "dokan_hisab_local_backup.json")
        }

    fun hasLocalBackup(): Boolean = localBackupFile.exists() && localBackupFile.length() > 0L

    suspend fun createLocalBackup(): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPayload()
            val json = adapter.toJson(payload)
            localBackupFile.writeText(json, Charsets.UTF_8)
            repository.updateLastBackupTimestamp(payload.createdAt)
            payload.createdAt
        }
    }

    suspend fun exportBackupToUri(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPayload()
            val json = adapter.toJson(payload)
            localBackupFile.writeText(json, Charsets.UTF_8)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: throw IllegalStateException("ফাইল সংরক্ষণ করা যায়নি")
            repository.updateLastBackupTimestamp(payload.createdAt)
            payload.createdAt
        }
    }

    suspend fun parseBackupFromUri(uri: Uri): Result<DokanBackupPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("ব্যাকআপ ফাইল পড়া যায়নি")
            adapter.fromJson(json) ?: throw IllegalArgumentException("ব্যাকআপ ফাইলটি সঠিক ফরম্যাটে নেই")
        }
    }

    suspend fun parseLocalBackup(): Result<DokanBackupPayload> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasLocalBackup()) throw IllegalStateException("কোনো লোকাল ব্যাকআপ পাওয়া যায়নি")
            val json = localBackupFile.readText(Charsets.UTF_8)
            adapter.fromJson(json) ?: throw IllegalArgumentException("ব্যাকআপ ফাইলটি সঠিক নয়")
        }
    }

    suspend fun restorePayload(payload: DokanBackupPayload): Result<Unit> {
        val customers = payload.customers.map {
            CustomerEntity(
                id = it.id,
                name = it.name,
                phone = it.phone,
                address = it.address,
                previousDue = it.previousDue,
                totalPurchase = it.totalPurchase,
                totalPaid = it.totalPaid,
                currentDue = it.currentDue,
                createdAt = it.createdAt
            )
        }
        val transactions = payload.transactions.map {
            val parsedType = try {
                TransactionType.valueOf(it.type)
            } catch (_: IllegalArgumentException) {
                TransactionType.CASH_SALE
            }
            TransactionEntity(
                id = it.id,
                customerId = it.customerId,
                customerName = it.customerName,
                type = parsedType,
                amount = it.amount,
                note = it.note,
                timestamp = it.timestamp,
                dateKey = it.dateKey
            )
        }
        val expenses = payload.expenses.map {
            ExpenseEntity(
                id = it.id,
                title = it.title,
                amount = it.amount,
                note = it.note,
                timestamp = it.timestamp,
                dateKey = it.dateKey
            )
        }
        val settings = ShopSettings(
            shopName = payload.settings.shopName,
            shopPhone = payload.settings.shopPhone,
            shopAddress = payload.settings.shopAddress,
            currencySymbol = payload.settings.currencySymbol,
            isDarkMode = payload.settings.isDarkMode
        )
        return repository.restoreAllData(customers, transactions, expenses, settings)
    }

    private suspend fun buildPayload(): DokanBackupPayload {
        val (customers, transactions, expenses) = repository.getSnapshotForBackup()
        val settings = repository.settingsFlow.first()
        return DokanBackupPayload(
            createdAt = System.currentTimeMillis(),
            settings = ShopSettingsBackupDto(
                shopName = settings.shopName,
                shopPhone = settings.shopPhone,
                shopAddress = settings.shopAddress,
                currencySymbol = settings.currencySymbol,
                isDarkMode = settings.isDarkMode
            ),
            customers = customers.map {
                CustomerBackupDto(
                    id = it.id,
                    name = it.name,
                    phone = it.phone,
                    address = it.address,
                    previousDue = it.previousDue,
                    totalPurchase = it.totalPurchase,
                    totalPaid = it.totalPaid,
                    currentDue = it.currentDue,
                    createdAt = it.createdAt
                )
            },
            transactions = transactions.map {
                TransactionBackupDto(
                    id = it.id,
                    customerId = it.customerId,
                    customerName = it.customerName,
                    type = it.type.name,
                    amount = it.amount,
                    note = it.note,
                    timestamp = it.timestamp,
                    dateKey = it.dateKey
                )
            },
            expenses = expenses.map {
                ExpenseBackupDto(
                    id = it.id,
                    title = it.title,
                    amount = it.amount,
                    note = it.note,
                    timestamp = it.timestamp,
                    dateKey = it.dateKey
                )
            }
        )
    }
}
