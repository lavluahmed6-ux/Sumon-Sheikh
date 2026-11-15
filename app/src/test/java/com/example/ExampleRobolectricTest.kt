package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DokanDatabase
import com.example.data.local.ShopPreferences
import com.example.data.local.TransactionType
import com.example.data.repository.DokanRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: DokanDatabase
    private lateinit var repository: DokanRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DokanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DokanRepository(
            database = database,
            dao = database.dokanDao(),
            preferences = ShopPreferences(context)
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `read bengali app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("দোকান হিসাব", appName)
    }

    @Test
    fun `credit purchase with partial payment and due collection updates balances accurately`() = runTest {
        val customerId = repository.addCustomer(
            name = "Rahim",
            phone = "01711000000",
            address = "Dhaka",
            previousDue = 0.0
        ).getOrThrow()

        // Purchase 1000, Paid 400 -> Remaining Due 600
        val creditResult = repository.recordCreditPurchase(
            customerId = customerId,
            totalPurchaseAmount = 1000.0,
            paidNowAmount = 400.0,
            note = "চাল ও ডাল"
        )
        assertTrue(creditResult.isSuccess)

        var rahim = repository.observeCustomer(customerId).first()!!
        assertEquals(1000.0, rahim.totalPurchase, 0.001)
        assertEquals(400.0, rahim.totalPaid, 0.001)
        assertEquals(600.0, rahim.currentDue, 0.001)

        // Attempt overpayment > 600 -> rejected
        val overpay = repository.recordPayment(customerId, 700.0, "অতিরিক্ত")
        assertTrue(overpay.isFailure)

        // Valid payment 250 -> remaining due 350
        assertTrue(repository.recordPayment(customerId, 250.0, "নগদ জমা").isSuccess)
        rahim = repository.observeCustomer(customerId).first()!!
        assertEquals(1000.0, rahim.totalPurchase, 0.001)
        assertEquals(650.0, rahim.totalPaid, 0.001)
        assertEquals(350.0, rahim.currentDue, 0.001)

        // Delete the payment -> due returns to 600
        val paymentTx = repository.observeCustomerTransactions(customerId).first()
            .first { it.type == TransactionType.PAYMENT }
        assertTrue(repository.deleteTransaction(paymentTx.id).isSuccess)
        rahim = repository.observeCustomer(customerId).first()!!
        assertEquals(600.0, rahim.currentDue, 0.001)
    }

    @Test
    fun `daily sales bundle and currency formatting work properly`() = runTest {
        val today = Formatters.todayDateKey()
        val result = repository.recordDailySalesBundle(
            dateKey = today,
            cashSales = 1500.0,
            creditSales = 500.0,
            customerId = null,
            expenseTitle = "বিদ্যুৎ বিল",
            expenseAmount = 300.0,
            note = "আজকের হিসাব"
        )
        assertTrue(result.isSuccess)

        val txs = repository.observeTransactionsByDate(today).first()
        val expenses = repository.observeExpensesByDate(today).first()
        assertEquals(2, txs.size)
        assertEquals(1, expenses.size)
        assertEquals(2000.0, txs.sumOf { it.amount }, 0.001)
        assertEquals(300.0, expenses.sumOf { it.amount }, 0.001)
        assertEquals("৳1,500", Formatters.formatCurrency(1500.0, "৳"))
    }
}
