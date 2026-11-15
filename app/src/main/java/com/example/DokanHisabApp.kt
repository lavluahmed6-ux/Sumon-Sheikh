package com.example

import android.app.Application
import com.example.data.backup.BackupManager
import com.example.data.local.DokanDatabase
import com.example.data.local.ShopPreferences
import com.example.data.report.PdfReportExporter
import com.example.data.repository.DokanRepository

class AppContainer(application: Application) {
    val database: DokanDatabase by lazy { DokanDatabase.getInstance(application) }
    val preferences: ShopPreferences by lazy { ShopPreferences(application) }
    val repository: DokanRepository by lazy {
        DokanRepository(
            database = database,
            dao = database.dokanDao(),
            preferences = preferences
        )
    }
    val backupManager: BackupManager by lazy { BackupManager(application, repository) }
    val pdfReportExporter: PdfReportExporter by lazy { PdfReportExporter(application) }
}

class DokanHisabApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
