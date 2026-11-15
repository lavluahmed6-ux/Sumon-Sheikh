package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    private val dateDisplayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val shortDayFormat = SimpleDateFormat("dd/MM", Locale.US)

    private val bengaliMonths = mapOf(
        0 to "জানু", 1 to "ফেব্রু", 2 to "মার্চ", 3 to "এপ্রিল",
        4 to "মে", 5 to "জুন", 6 to "জুলাই", 7 to "আগস্ট",
        8 to "সেপ্টে", 9 to "অক্টো", 10 to "নভে", 11 to "ডিসে"
    )

    private val bengaliWeekdays = mapOf(
        Calendar.SATURDAY to "শনি",
        Calendar.SUNDAY to "রবি",
        Calendar.MONDAY to "সোম",
        Calendar.TUESDAY to "মঙ্গল",
        Calendar.WEDNESDAY to "বুধ",
        Calendar.THURSDAY to "বৃহঃ",
        Calendar.FRIDAY to "শুক্র"
    )

    fun todayDateKey(): String = keyFormat.format(Date())

    fun dateKeyFromMillis(millis: Long): String = keyFormat.format(Date(millis))

    fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return keyFormat.format(cal.time)
    }

    fun daysAgoDateKey(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return keyFormat.format(cal.time)
    }

    fun currentMonthStartKey(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return keyFormat.format(cal.time)
    }

    fun parseDateKeyToMillis(dateKey: String): Long {
        return try {
            keyFormat.parse(dateKey)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun shiftDateKey(dateKey: String, deltaDays: Int): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = parseDateKeyToMillis(dateKey)
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        return keyFormat.format(cal.time)
    }

    fun formatDateBn(dateKey: String): String {
        val today = todayDateKey()
        val yesterday = yesterdayDateKey()
        val cal = Calendar.getInstance().apply { timeInMillis = parseDateKeyToMillis(dateKey) }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = bengaliMonths[cal.get(Calendar.MONTH)] ?: ""
        val year = cal.get(Calendar.YEAR)
        val base = "$day $month $year"
        return when (dateKey) {
            today -> "আজ • $base"
            yesterday -> "গতকাল • $base"
            else -> base
        }
    }

    fun formatShortDayLabel(dateKey: String): String {
        val cal = Calendar.getInstance().apply { timeInMillis = parseDateKeyToMillis(dateKey) }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val weekday = bengaliWeekdays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        return "$weekday\n$day"
    }

    fun formatDateTimeDisplay(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = bengaliMonths[cal.get(Calendar.MONTH)] ?: ""
        val year = cal.get(Calendar.YEAR)
        val time = timeFormat.format(Date(millis))
        return "$day $month $year • $time"
    }

    fun formatPdfDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun formatPdfDate(dateKey: String): String {
        return try {
            val d = keyFormat.parse(dateKey) ?: return dateKey
            dateDisplayFormat.format(d)
        } catch (_: Exception) {
            dateKey
        }
    }

    fun formatShortDate(dateKey: String): String {
        return try {
            val d = keyFormat.parse(dateKey) ?: return dateKey
            shortDayFormat.format(d)
        } catch (_: Exception) {
            dateKey
        }
    }

    fun formatCurrency(amount: Double, symbol: String = "৳"): String {
        val safe = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        val sign = if (safe < 0) "-" else ""
        val absVal = abs(safe)
        val formatted = formatIndianGrouping(absVal)
        return "$sign$symbol$formatted"
    }

    fun formatAmountPlain(amount: Double): String {
        val safe = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        return if (safe % 1.0 == 0.0) {
            safe.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", safe)
        }
    }

    private fun formatIndianGrouping(value: Double): String {
        val hasDecimals = (value * 100).toLong() % 100L != 0L
        val integerPart = value.toLong()
        val intStr = integerPart.toString()
        val groupedInt = if (intStr.length <= 3) {
            intStr
        } else {
            val lastThree = intStr.takeLast(3)
            val rest = intStr.dropLast(3)
            val pairs = rest.reversed().chunked(2).joinToString(",").reversed()
            "$pairs,$lastThree"
        }
        return if (hasDecimals) {
            val symbols = DecimalFormatSymbols(Locale.US)
            val dec = DecimalFormat(".00", symbols).format(value - integerPart)
            "$groupedInt$dec"
        } else {
            groupedInt
        }
    }
}
