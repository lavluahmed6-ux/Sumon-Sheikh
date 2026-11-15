package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType(val labelBn: String) {
    CREDIT_SALE("বাকি"),
    PAYMENT("জমা"),
    CASH_SALE("নগদ বিক্রি")
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["dateKey"]),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val customerId: Long? = null,
    val customerName: String,
    val type: TransactionType,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateKey: String
)
