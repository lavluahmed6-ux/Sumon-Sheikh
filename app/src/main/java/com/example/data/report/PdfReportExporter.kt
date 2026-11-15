package com.example.data.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.CustomerEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ShopSettings
import com.example.data.local.TransactionEntity
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ReportSummaryData(
    val periodTitle: String,
    val totalSales: Double,
    val cashSales: Double,
    val creditSales: Double,
    val paymentCollected: Double,
    val totalExpenses: Double,
    val netCashBalance: Double,
    val totalStoreDue: Double,
    val totalCustomers: Int,
    val customersWithDue: List<CustomerEntity>,
    val transactions: List<TransactionEntity>,
    val expenses: List<ExpenseEntity>
)

class PdfReportExporter(private val context: Context) {

    suspend fun exportToUri(
        uri: Uri,
        settings: ShopSettings,
        report: ReportSummaryData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val document = buildPdfDocument(settings, report)
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    document.writeTo(out)
                    out.flush()
                } ?: throw IllegalStateException("PDF ফাইল তৈরি করা যায়নি")
            } finally {
                document.close()
            }
        }
    }

    suspend fun createShareablePdfUri(
        settings: ShopSettings,
        report: ReportSummaryData
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "dokan_hisab_report_${System.currentTimeMillis()}.pdf")
            val document = buildPdfDocument(settings, report)
            try {
                FileOutputStream(file).use { out ->
                    document.writeTo(out)
                    out.flush()
                }
            } finally {
                document.close()
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

    private fun buildPdfDocument(
        settings: ShopSettings,
        report: ReportSummaryData
    ): PdfDocument {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - margin * 2

        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 48f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A4B5D")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 11f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1565C0")
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#EFF6FF")
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > pageHeight - 44f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 44f
            }
        }

        // Header Banner
        canvas.drawRect(margin, y - 18f, margin + contentWidth, y + 54f, headerBgPaint)
        canvas.drawText(settings.shopName, margin + 12f, y + 4f, titlePaint)
        val contactLine = listOfNotNull(
            settings.shopPhone.takeIf { it.isNotBlank() },
            settings.shopAddress.takeIf { it.isNotBlank() }
        ).joinToString(" | ").ifBlank { "Smart Dokan Hisab Offline Report" }
        canvas.drawText(contactLine, margin + 12f, y + 22f, subtitlePaint)
        canvas.drawText(
            "${report.periodTitle} • ${Formatters.formatPdfDateTime(System.currentTimeMillis())}",
            margin + 12f,
            y + 39f,
            subtitlePaint
        )
        y += 74f

        // Financial Summary
        canvas.drawText("সারসংক্ষেপ (Business Summary)", margin, y, sectionPaint)
        y += 10f
        canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
        y += 18f

        val sym = settings.currencySymbol
        val summaryRows = listOf(
            "মোট বিক্রি (Total Sales)" to Formatters.formatCurrency(report.totalSales, sym),
            "নগদ বিক্রি (Cash Sales)" to Formatters.formatCurrency(report.cashSales, sym),
            "বাকি বিক্রি (Credit Sales)" to Formatters.formatCurrency(report.creditSales, sym),
            "টাকা জমা / আদায় (Collection)" to Formatters.formatCurrency(report.paymentCollected, sym),
            "মোট খরচ (Total Expenses)" to Formatters.formatCurrency(report.totalExpenses, sym),
            "নিট নগদ ব্যালেন্স (Net Cash)" to Formatters.formatCurrency(report.netCashBalance, sym),
            "দোকানের মোট বাকি (Total Due)" to Formatters.formatCurrency(report.totalStoreDue, sym)
        )
        summaryRows.forEach { (label, value) ->
            ensureSpace(20f)
            canvas.drawText(label, margin + 6f, y, bodyPaint)
            val valWidth = boldBodyPaint.measureText(value)
            canvas.drawText(value, margin + contentWidth - valWidth - 6f, y, boldBodyPaint)
            y += 18f
        }
        y += 12f

        // Customer Due List
        if (report.customersWithDue.isNotEmpty()) {
            ensureSpace(44f)
            canvas.drawText("কাস্টমার বাকি তালিকা (${report.customersWithDue.size})", margin, y, sectionPaint)
            y += 10f
            canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
            y += 18f

            report.customersWithDue.take(35).forEachIndexed { idx, c ->
                ensureSpace(18f)
                val left = "${idx + 1}. ${c.name} (${c.phone})"
                val right = Formatters.formatCurrency(c.currentDue, sym)
                canvas.drawText(left.take(48), margin + 6f, y, bodyPaint)
                val rw = boldBodyPaint.measureText(right)
                canvas.drawText(right, margin + contentWidth - rw - 6f, y, boldBodyPaint)
                y += 16f
            }
            y += 12f
        }

        // Recent Transactions
        if (report.transactions.isNotEmpty()) {
            ensureSpace(44f)
            canvas.drawText("লেনদেনের বিবরণ (${report.transactions.size})", margin, y, sectionPaint)
            y += 10f
            canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
            y += 18f

            report.transactions.take(50).forEach { tx ->
                ensureSpace(18f)
                val dateText = Formatters.formatPdfDate(tx.dateKey)
                val left = "$dateText | ${tx.type.labelBn} | ${tx.customerName}"
                val right = Formatters.formatCurrency(tx.amount, sym)
                canvas.drawText(left.take(52), margin + 6f, y, bodyPaint)
                val rw = boldBodyPaint.measureText(right)
                canvas.drawText(right, margin + contentWidth - rw - 6f, y, boldBodyPaint)
                y += 16f
            }
            y += 12f
        }

        // Expenses
        if (report.expenses.isNotEmpty()) {
            ensureSpace(44f)
            canvas.drawText("খরচের তালিকা (${report.expenses.size})", margin, y, sectionPaint)
            y += 10f
            canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
            y += 18f

            report.expenses.take(30).forEach { exp ->
                ensureSpace(18f)
                val left = "${Formatters.formatPdfDate(exp.dateKey)} | ${exp.title}"
                val right = Formatters.formatCurrency(exp.amount, sym)
                canvas.drawText(left.take(52), margin + 6f, y, bodyPaint)
                val rw = boldBodyPaint.measureText(right)
                canvas.drawText(right, margin + contentWidth - rw - 6f, y, boldBodyPaint)
                y += 16f
            }
        }

        document.finishPage(page)
        return document
    }
}
