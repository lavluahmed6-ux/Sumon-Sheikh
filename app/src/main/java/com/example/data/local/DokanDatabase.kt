package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class DokanConverters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (_: IllegalArgumentException) {
            TransactionType.CASH_SALE
        }
    }
}

@Database(
    entities = [CustomerEntity::class, TransactionEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DokanConverters::class)
abstract class DokanDatabase : RoomDatabase() {
    abstract fun dokanDao(): DokanDao

    companion object {
        @Volatile
        private var INSTANCE: DokanDatabase? = null

        fun getInstance(context: Context): DokanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DokanDatabase::class.java,
                    "smart_dokan_hisab.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
