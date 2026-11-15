package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.CustomerEntity
import com.example.data.local.DokanDao
import com.example.data.local.DokanDatabase
import com.example.data.local.ExpenseEntity
import com.example.data.local.ShopPreferences
import com.example.data.local.ShopSettings
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.util.Formatters
import kotlinx.coroutines.flow.Flow

class DokanRepository(
    private val database: DokanDatabase,
    private val dao: DokanDao,
    private val preferences: ShopPreferences
) {
    val customersFlow: Flow<List<CustomerEntity>> = dao.observeAllCustomers()
    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val expensesFlow: Flow<List<ExpenseEntity>> = dao.observeAllExpenses()
    val settingsFlow: Flow<ShopSettings> = preferences.settingsFlow

    fun observeCustomer(customerId: Long): Flow<CustomerEntity?> = dao.observeCustomerById(customerId)

    fun observeCustomerTransactions(customerId: Long): Flow<List<TransactionEntity>> =
        dao.observeTransactionsForCustomer(customerId)

    fun observeTransactionsByDate(dateKey: String): Flow<List<TransactionEntity>> =
        dao.observeTransactionsByDate(dateKey)

    fun observeExpensesByDate(dateKey: String): Flow<List<ExpenseEntity>> =
        dao.observeExpensesByDate(dateKey)

    suspend fun addCustomer(
        name: String,
        phone: String,
        address: String,
        previousDue: Double
    ): Result<Long> {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("কাস্টমারের নাম লিখুন"))
        if (cleanPhone.isEmpty()) return Result.failure(IllegalArgumentException("ফোন নম্বর লিখুন"))
        if (previousDue < 0.0) return Result.failure(IllegalArgumentException("আগের বাকি ঋণাত্মক হতে পারবে না"))

        return runCatching {
            database.withTransaction {
                val entity = CustomerEntity(
                    name = cleanName,
                    phone = cleanPhone,
                    address = address.trim(),
                    previousDue = previousDue,
                    totalPurchase = 0.0,
                    totalPaid = 0.0,
                    currentDue = previousDue
                )
                dao.insertCustomer(entity)
            }
        }
    }

    suspend fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String,
        address: String,
        previousDue: Double
    ): Result<Unit> {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("কাস্টমারের নাম লিখুন"))
        if (cleanPhone.isEmpty()) return Result.failure(IllegalArgumentException("ফোন নম্বর লিখুন"))
        if (previousDue < 0.0) return Result.failure(IllegalArgumentException("আগের বাকি ঋণাত্মক হতে পারবে না"))

        return runCatching {
            database.withTransaction {
                val existing = dao.getCustomerById(customerId)
                    ?: throw IllegalStateException("কাস্টমার পাওয়া যায়নি")
                val updated = existing.copy(
                    name = cleanName,
                    phone = cleanPhone,
                    address = address.trim(),
                    previousDue = previousDue
                )
                dao.updateCustomer(updated)
                if (existing.name != cleanName) {
                    dao.updateCustomerNameInTransactions(customerId, cleanName)
                }
                recalculateCustomerBalancesLocked(customerId)
            }
        }
    }

    suspend fun deleteCustomer(customerId: Long): Result<Unit> {
        return runCatching {
            database.withTransaction {
                dao.deleteTransactionsByCustomerId(customerId)
                dao.deleteCustomerById(customerId)
            }
        }
    }

    /**
     * Records a customer credit purchase, optionally including an immediate partial cash payment
     * (e.g. Purchase ৳1,000, Paid ৳400 -> Cash Sale ৳400 + Credit Sale ৳600).
     */
    suspend fun recordCreditPurchase(
        customerId: Long,
        totalPurchaseAmount: Double,
        paidNowAmount: Double = 0.0,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Result<Unit> {
        if (totalPurchaseAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("বিক্রির পরিমাণ ০-এর বেশি হতে হবে"))
        }
        if (paidNowAmount < 0.0 || paidNowAmount > totalPurchaseAmount) {
            return Result.failure(IllegalArgumentException("জমার পরিমাণ মোট বিলের বেশি হতে পারবে না"))
        }

        return runCatching {
            database.withTransaction {
                val customer = dao.getCustomerById(customerId)
                    ?: throw IllegalStateException("কাস্টমার পাওয়া যায়নি")
                val dateKey = Formatters.dateKeyFromMillis(timestamp)
                val cleanNote = note.trim()
                val creditPortion = totalPurchaseAmount - paidNowAmount

                if (paidNowAmount > 0.0) {
                    val cashNote = buildString {
                        if (cleanNote.isNotEmpty()) append(cleanNote).append(" • ")
                        append("কেনার সময় নগদ জমা")
                        if (creditPortion > 0.0) {
                            append(" (মোট ").append(Formatters.formatAmountPlain(totalPurchaseAmount)).append(")")
                        }
                    }
                    dao.insertTransaction(
                        TransactionEntity(
                            customerId = customer.id,
                            customerName = customer.name,
                            type = TransactionType.CASH_SALE,
                            amount = paidNowAmount,
                            note = cashNote,
                            timestamp = timestamp,
                            dateKey = dateKey
                        )
                    )
                }

                if (creditPortion > 0.0) {
                    val creditNote = buildString {
                        if (cleanNote.isNotEmpty()) append(cleanNote)
                        if (paidNowAmount > 0.0) {
                            if (isNotEmpty()) append(" • ")
                            append("মোট ${Formatters.formatAmountPlain(totalPurchaseAmount)}, নগদ ${Formatters.formatAmountPlain(paidNowAmount)}")
                        }
                    }
                    dao.insertTransaction(
                        TransactionEntity(
                            customerId = customer.id,
                            customerName = customer.name,
                            type = TransactionType.CREDIT_SALE,
                            amount = creditPortion,
                            note = creditNote,
                            timestamp = timestamp + 1L,
                            dateKey = dateKey
                        )
                    )
                }

                recalculateCustomerBalancesLocked(customer.id)
            }
        }
    }

    suspend fun recordPayment(
        customerId: Long,
        amount: Double,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Result<Unit> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("জমার পরিমাণ ০-এর বেশি হতে হবে"))
        }

        return runCatching {
            database.withTransaction {
                val customer = dao.getCustomerById(customerId)
                    ?: throw IllegalStateException("কাস্টমার পাওয়া যায়নি")
                if (amount > customer.currentDue + 0.001) {
                    throw IllegalArgumentException(
                        "বাকি পরিমাণ (${Formatters.formatAmountPlain(customer.currentDue)}) এর বেশি টাকা জমা নেওয়া যাবে না"
                    )
                }
                val dateKey = Formatters.dateKeyFromMillis(timestamp)
                dao.insertTransaction(
                    TransactionEntity(
                        customerId = customer.id,
                        customerName = customer.name,
                        type = TransactionType.PAYMENT,
                        amount = amount,
                        note = note.trim().ifBlank { "বাকি পরিশোধ" },
                        timestamp = timestamp,
                        dateKey = dateKey
                    )
                )
                recalculateCustomerBalancesLocked(customer.id)
            }
        }
    }

    suspend fun recordCashSale(
        customerId: Long? = null,
        amount: Double,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Result<Unit> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("বিক্রির পরিমাণ ০-এর বেশি হতে হবে"))
        }

        return runCatching {
            database.withTransaction {
                val customer = customerId?.let { dao.getCustomerById(it) }
                val dateKey = Formatters.dateKeyFromMillis(timestamp)
                dao.insertTransaction(
                    TransactionEntity(
                        customerId = customer?.id,
                        customerName = customer?.name ?: "নগদ ক্রেতা",
                        type = TransactionType.CASH_SALE,
                        amount = amount,
                        note = note.trim().ifBlank { "নগদ বিক্রি" },
                        timestamp = timestamp,
                        dateKey = dateKey
                    )
                )
                if (customer != null) {
                    recalculateCustomerBalancesLocked(customer.id)
                }
            }
        }
    }

    suspend fun recordDailySalesBundle(
        dateKey: String,
        cashSales: Double,
        creditSales: Double,
        customerId: Long?,
        expenseTitle: String,
        expenseAmount: Double,
        note: String
    ): Result<Unit> {
        if (cashSales < 0.0 || creditSales < 0.0 || expenseAmount < 0.0) {
            return Result.failure(IllegalArgumentException("পরিমাণ ঋণাত্মক হতে পারবে না"))
        }
        if (cashSales == 0.0 && creditSales == 0.0 && expenseAmount == 0.0) {
            return Result.failure(IllegalArgumentException("কমপক্ষে একটি পরিমাণ লিখুন"))
        }

        return runCatching {
            database.withTransaction {
                val baseMillis = alignTimestampToDateKey(dateKey)
                val cleanNote = note.trim()
                val customer = customerId?.let { dao.getCustomerById(it) }

                if (cashSales > 0.0) {
                    dao.insertTransaction(
                        TransactionEntity(
                            customerId = customer?.id,
                            customerName = customer?.name ?: "দৈনিক নগদ বিক্রি",
                            type = TransactionType.CASH_SALE,
                            amount = cashSales,
                            note = cleanNote.ifBlank { "দৈনিক নগদ বিক্রি" },
                            timestamp = baseMillis,
                            dateKey = dateKey
                        )
                    )
                }

                if (creditSales > 0.0) {
                    dao.insertTransaction(
                        TransactionEntity(
                            customerId = customer?.id,
                            customerName = customer?.name ?: "সাধারণ বাকি বিক্রি",
                            type = TransactionType.CREDIT_SALE,
                            amount = creditSales,
                            note = cleanNote.ifBlank { "দৈনিক বাকি বিক্রি" },
                            timestamp = baseMillis + 1L,
                            dateKey = dateKey
                        )
                    )
                }

                if (customer != null && (cashSales > 0.0 || creditSales > 0.0)) {
                    recalculateCustomerBalancesLocked(customer.id)
                }

                if (expenseAmount > 0.0) {
                    dao.insertExpense(
                        ExpenseEntity(
                            title = expenseTitle.trim().ifBlank { "দোকান খরচ" },
                            amount = expenseAmount,
                            note = cleanNote,
                            timestamp = baseMillis + 2L,
                            dateKey = dateKey
                        )
                    )
                }
            }
        }
    }

    suspend fun updateTransaction(
        transactionId: Long,
        newType: TransactionType,
        newAmount: Double,
        newNote: String,
        newTimestamp: Long
    ): Result<Unit> {
        if (newAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("পরিমাণ ০-এর বেশি হতে হবে"))
        }

        return runCatching {
            database.withTransaction {
                val existing = dao.getTransactionById(transactionId)
                    ?: throw IllegalStateException("লেনদেন পাওয়া যায়নি")
                val updated = existing.copy(
                    type = newType,
                    amount = newAmount,
                    note = newNote.trim(),
                    timestamp = newTimestamp,
                    dateKey = Formatters.dateKeyFromMillis(newTimestamp)
                )
                dao.updateTransaction(updated)

                val cid = existing.customerId
                if (cid != null) {
                    val customer = dao.getCustomerById(cid)
                    if (customer != null) {
                        val txs = dao.getTransactionsForCustomerOnce(cid)
                        val creditSum = txs.filter { it.type == TransactionType.CREDIT_SALE }.sumOf { it.amount }
                        val paymentSum = txs.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }
                        val rawDue = customer.previousDue + creditSum - paymentSum
                        if (rawDue < -0.001) {
                            throw IllegalArgumentException("এই পরিবর্তনে কাস্টমারের ব্যালেন্স ঋণাত্মক হয়ে যাবে")
                        }
                        recalculateCustomerBalancesLocked(cid)
                    }
                }
            }
        }
    }

    suspend fun deleteTransaction(transactionId: Long): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val existing = dao.getTransactionById(transactionId) ?: return@withTransaction
                dao.deleteTransactionById(transactionId)
                existing.customerId?.let { recalculateCustomerBalancesLocked(it) }
            }
        }
    }

    suspend fun addExpense(
        title: String,
        amount: Double,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Result<Unit> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("খরচের পরিমাণ ০-এর বেশি হতে হবে"))
        }
        val cleanTitle = title.trim().ifBlank { "দোকান খরচ" }
        return runCatching {
            database.withTransaction {
                dao.insertExpense(
                    ExpenseEntity(
                        title = cleanTitle,
                        amount = amount,
                        note = note.trim(),
                        timestamp = timestamp,
                        dateKey = Formatters.dateKeyFromMillis(timestamp)
                    )
                )
            }
        }
    }

    suspend fun deleteExpense(expenseId: Long): Result<Unit> {
        return runCatching {
            database.withTransaction {
                dao.deleteExpenseById(expenseId)
            }
        }
    }

    suspend fun getSnapshotForBackup(): Triple<List<CustomerEntity>, List<TransactionEntity>, List<ExpenseEntity>> {
        return Triple(
            dao.getAllCustomersOnce(),
            dao.getAllTransactionsOnce(),
            dao.getAllExpensesOnce()
        )
    }

    suspend fun restoreAllData(
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        expenses: List<ExpenseEntity>,
        restoredSettings: ShopSettings?
    ): Result<Unit> {
        return runCatching {
            database.withTransaction {
                dao.clearAllTransactions()
                dao.clearAllExpenses()
                dao.clearAllCustomers()

                if (customers.isNotEmpty()) dao.insertCustomers(customers)
                if (transactions.isNotEmpty()) dao.insertTransactions(transactions)
                if (expenses.isNotEmpty()) dao.insertExpenses(expenses)

                customers.forEach { recalculateCustomerBalancesLocked(it.id) }
            }
            if (restoredSettings != null) {
                preferences.applyRestoredSettings(restoredSettings)
            }
        }
    }

    suspend fun updateShopProfile(name: String, phone: String, address: String) =
        preferences.updateShopProfile(name, phone, address)

    suspend fun updateCurrency(symbol: String) = preferences.updateCurrency(symbol)

    suspend fun setDarkMode(enabled: Boolean) = preferences.setDarkMode(enabled)

    suspend fun updateLastBackupTimestamp(timestamp: Long) =
        preferences.updateLastBackupTimestamp(timestamp)

    private suspend fun recalculateCustomerBalancesLocked(customerId: Long) {
        val customer = dao.getCustomerById(customerId) ?: return
        val txs = dao.getTransactionsForCustomerOnce(customerId)
        val creditSales = txs.filter { it.type == TransactionType.CREDIT_SALE }.sumOf { it.amount }
        val cashSales = txs.filter { it.type == TransactionType.CASH_SALE }.sumOf { it.amount }
        val payments = txs.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }

        val totalPurchase = creditSales + cashSales
        val totalPaid = payments + cashSales
        val currentDue = (customer.previousDue + creditSales - payments).coerceAtLeast(0.0)

        dao.updateCustomer(
            customer.copy(
                totalPurchase = totalPurchase,
                totalPaid = totalPaid,
                currentDue = currentDue
            )
        )
    }

    private fun alignTimestampToDateKey(dateKey: String): Long {
        val today = Formatters.todayDateKey()
        if (dateKey == today) return System.currentTimeMillis()
        // Set noon on the selected historical date so timezone offsets keep the same dateKey
        return Formatters.parseDateKeyToMillis(dateKey) + 12 * 60 * 60 * 1000L
    }
}
