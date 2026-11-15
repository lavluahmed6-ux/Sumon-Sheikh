package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String,
    val address: String = "",
    val previousDue: Double = 0.0,
    val totalPurchase: Double = 0.0,
    val totalPaid: Double = 0.0,
    val currentDue: Double = previousDue,
    val createdAt: Long = System.currentTimeMillis()
)
