package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.CustomerEntity
import com.example.ui.components.CashSaleDialog
import com.example.ui.components.CreditSaleDialog
import com.example.ui.components.CustomerFormDialog
import com.example.ui.components.ReceivePaymentDialog
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DueScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.DokanHisabTheme
import com.example.ui.viewmodel.AppViewModelFactory
import com.example.ui.viewmodel.CustomersViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.ReportsViewModel
import com.example.ui.viewmodel.SalesHistoryViewModel
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val CUSTOMERS = "customers"
    const val SALES = "sales"
    const val DUE = "due"
    const val SETTINGS = "settings"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"

    fun customerDetail(customerId: Long): String = "customer_detail/$customerId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomTabs = listOf(
    BottomNavTab(Routes.HOME, "হোম", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavTab(Routes.CUSTOMERS, "কাস্টমার", Icons.Filled.Group, Icons.Outlined.Group),
    BottomNavTab(Routes.SALES, "বিক্রি", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    BottomNavTab(Routes.DUE, "বাকি", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BottomNavTab(Routes.SETTINGS, "সেটিংস", Icons.Filled.Settings, Icons.Outlined.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DokanHisabApp
        val factory = AppViewModelFactory(app.container)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            DokanHisabTheme(darkTheme = settingsState.settings.isDarkMode) {
                DokanHisabRoot(
                    factory = factory,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
private fun DokanHisabRoot(
    factory: AppViewModelFactory,
    settingsViewModel: SettingsViewModel
) {
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val customersViewModel: CustomersViewModel = viewModel(factory = factory)
    val salesHistoryViewModel: SalesHistoryViewModel = viewModel(factory = factory)
    val reportsViewModel: ReportsViewModel = viewModel(factory = factory)

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val customersState by customersViewModel.uiState.collectAsStateWithLifecycle()
    val dailySalesState by salesHistoryViewModel.dailyState.collectAsStateWithLifecycle()
    val historyState by salesHistoryViewModel.historyState.collectAsStateWithLifecycle()
    val reportsState by reportsViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomOrRail = currentRoute in bottomTabs.map { it.route }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Shared quick modal states
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var creditSalePreselected by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCreditSaleDialog by remember { mutableStateOf(false) }
    var paymentPreselected by remember { mutableStateOf<CustomerEntity?>(null) }
    var showReceivePaymentDialog by remember { mutableStateOf(false) }
    var showCashSaleDialog by remember { mutableStateOf(false) }

    val currencySymbol = settingsState.settings.currencySymbol

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showBottomOrRail && !useRail) {
                    NavigationBar {
                        bottomTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showBottomOrRail && useRail) {
                    NavigationRail {
                        bottomTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier.widthIn(max = 860.dp)
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                uiState = homeState,
                                onOpenNewCustomer = { showAddCustomerDialog = true },
                                onOpenCashSale = { showCashSaleDialog = true },
                                onOpenCreditSale = {
                                    creditSalePreselected = null
                                    showCreditSaleDialog = true
                                },
                                onOpenReceivePayment = {
                                    if (customersState.allCustomers.isEmpty()) {
                                        showMessage("প্রথমে কাস্টমার যোগ করুন")
                                    } else {
                                        paymentPreselected = null
                                        showReceivePaymentDialog = true
                                    }
                                },
                                onNavigateCustomers = { navigateTopTab(Routes.CUSTOMERS) },
                                onNavigateSales = { navigateTopTab(Routes.SALES) },
                                onNavigateDue = { navigateTopTab(Routes.DUE) },
                                onNavigateHistory = { navController.navigate(Routes.TRANSACTIONS) },
                                onNavigateReports = { navController.navigate(Routes.REPORTS) },
                                onOpenCustomerDetail = { cid ->
                                    navController.navigate(Routes.customerDetail(cid))
                                }
                            )
                        }

                        composable(Routes.CUSTOMERS) {
                            CustomersScreen(
                                uiState = customersState,
                                onSearchChange = customersViewModel::setSearchQuery,
                                onAddCustomer = { showAddCustomerDialog = true },
                                onCustomerClick = { cid ->
                                    navController.navigate(Routes.customerDetail(cid))
                                }
                            )
                        }

                        composable(Routes.SALES) {
                            SalesScreen(
                                uiState = dailySalesState,
                                onSelectDate = salesHistoryViewModel::setSelectedDate,
                                onShiftDate = salesHistoryViewModel::shiftSelectedDate,
                                onOpenCashSale = { showCashSaleDialog = true },
                                onOpenCreditSale = {
                                    creditSalePreselected = null
                                    showCreditSaleDialog = true
                                },
                                onRecordDailyBundle = { cash, credit, cid, expTitle, expAmt, note ->
                                    salesHistoryViewModel.recordDailySalesBundle(
                                        dateKey = dailySalesState.selectedDateKey,
                                        cashSales = cash,
                                        creditSales = credit,
                                        customerId = cid,
                                        expenseTitle = expTitle,
                                        expenseAmount = expAmt,
                                        note = note
                                    ) { res ->
                                        res.onSuccess { showMessage("দৈনিক হিসাব সংরক্ষিত হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                                    }
                                },
                                onAddExpense = { title, amt, note ->
                                    salesHistoryViewModel.addExpense(
                                        title = title,
                                        amount = amt,
                                        note = note,
                                        dateKey = dailySalesState.selectedDateKey
                                    ) { res ->
                                        res.onSuccess { showMessage("খরচ যোগ হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                                    }
                                },
                                onDeleteExpense = { id ->
                                    salesHistoryViewModel.deleteExpense(id) { res ->
                                        res.onSuccess { showMessage("খরচ মুছে ফেলা হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                                    }
                                },
                                onUpdateTransaction = { tx, type, amt, note ->
                                    salesHistoryViewModel.updateTransaction(tx.id, type, amt, note, tx.timestamp) { res ->
                                        res.onSuccess { showMessage("লেনদেন আপডেট হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "আপডেট করা যায়নি") }
                                    }
                                },
                                onDeleteTransaction = { txId ->
                                    salesHistoryViewModel.deleteTransaction(txId) { res ->
                                        res.onSuccess { showMessage("লেনদেন মুছে ফেলা হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "মুছে ফেলা যায়নি") }
                                    }
                                },
                                onNavigateHistory = { navController.navigate(Routes.TRANSACTIONS) },
                                onNavigateReports = { navController.navigate(Routes.REPORTS) }
                            )
                        }

                        composable(Routes.DUE) {
                            DueScreen(
                                uiState = customersState,
                                onSearchChange = customersViewModel::setDueSearchQuery,
                                onSortChange = customersViewModel::setDueSortOption,
                                onCustomerClick = { cid ->
                                    navController.navigate(Routes.customerDetail(cid))
                                },
                                onReceivePaymentFor = { customer ->
                                    paymentPreselected = customer
                                    showReceivePaymentDialog = true
                                },
                                onAddCreditFor = { customer ->
                                    creditSalePreselected = customer
                                    showCreditSaleDialog = true
                                }
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                uiState = settingsState,
                                onUpdateShopProfile = { name, phone, address ->
                                    settingsViewModel.updateShopProfile(name, phone, address) {
                                        showMessage("দোকানের তথ্য সংরক্ষিত হয়েছে")
                                    }
                                },
                                onUpdateCurrency = settingsViewModel::updateCurrency,
                                onToggleDarkMode = settingsViewModel::setDarkMode,
                                onCreateLocalBackup = {
                                    settingsViewModel.createLocalBackup { res ->
                                        res.onSuccess { showMessage("লোকাল ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "ব্যাকআপ ব্যর্থ হয়েছে") }
                                    }
                                },
                                onExportBackupToUri = { uri ->
                                    settingsViewModel.exportBackupToUri(uri) { res ->
                                        res.onSuccess { showMessage("ব্যাকআপ ফাইল এক্সপোর্ট সম্পন্ন হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "এক্সপোর্ট ব্যর্থ হয়েছে") }
                                    }
                                },
                                onSelectRestoreFileUri = { uri ->
                                    settingsViewModel.prepareRestoreFromUri(uri) { err ->
                                        showMessage(err.message ?: "ফাইল পড়া যায়নি")
                                    }
                                },
                                onRequestLocalRestore = {
                                    settingsViewModel.prepareRestoreFromLocal { err ->
                                        showMessage(err.message ?: "লোকাল ব্যাকআপ পাওয়া যায়নি")
                                    }
                                },
                                onConfirmRestore = {
                                    settingsViewModel.confirmRestore { res ->
                                        res.onSuccess { showMessage("সব ডেটা সফলভাবে রিস্টোর হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "রিস্টোর ব্যর্থ হয়েছে") }
                                    }
                                },
                                onDismissRestorePreview = settingsViewModel::dismissRestorePreview,
                                onNavigateReports = { navController.navigate(Routes.REPORTS) }
                            )
                        }

                        composable(
                            route = Routes.CUSTOMER_DETAIL,
                            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val customerId = backStackEntry.arguments?.getLong("customerId") ?: -1L
                            val customer by customersViewModel.observeCustomer(customerId)
                                .collectAsStateWithLifecycle(initialValue = null)
                            val txs by customersViewModel.observeCustomerTransactions(customerId)
                                .collectAsStateWithLifecycle(initialValue = emptyList())

                            CustomerDetailScreen(
                                customer = customer,
                                transactions = txs,
                                currencySymbol = currencySymbol,
                                onBack = { navController.popBackStack() },
                                onUpdateCustomer = { name, phone, address, prevDue ->
                                    customersViewModel.updateCustomer(customerId, name, phone, address, prevDue) { res ->
                                        res.onSuccess { showMessage("কাস্টমার তথ্য আপডেট হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "আপডেট ব্যর্থ") }
                                    }
                                },
                                onDeleteCustomer = {
                                    customersViewModel.deleteCustomer(customerId) { res ->
                                        res.onSuccess {
                                            showMessage("কাস্টমার মুছে ফেলা হয়েছে")
                                            navController.popBackStack()
                                        }.onFailure { showMessage(it.message ?: "মুছে ফেলা যায়নি") }
                                    }
                                },
                                onCreditSale = { purchase, paidNow, note ->
                                    customersViewModel.recordCreditPurchase(customerId, purchase, paidNow, note) { res ->
                                        res.onSuccess { showMessage("বাকি বিক্রি সংরক্ষিত হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                                    }
                                },
                                onReceivePayment = { amt, note ->
                                    customersViewModel.recordPayment(customerId, amt, note) { res ->
                                        res.onSuccess { showMessage("টাকা জমা সংরক্ষিত হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                                    }
                                },
                                onCashSale = { amt, note ->
                                    customersViewModel.recordCashSale(customerId, amt, note) { res ->
                                        res.onSuccess { showMessage("নগদ বিক্রি সংরক্ষিত হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                                    }
                                },
                                onUpdateTransaction = { tx, type, amt, note ->
                                    customersViewModel.updateTransaction(tx.id, type, amt, note, tx.timestamp) { res ->
                                        res.onSuccess { showMessage("লেনদেন আপডেট হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "আপডেট করা যায়নি") }
                                    }
                                },
                                onDeleteTransaction = { txId ->
                                    customersViewModel.deleteTransaction(txId) { res ->
                                        res.onSuccess { showMessage("লেনদেন মুছে ফেলা হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "মুছে ফেলা যায়নি") }
                                    }
                                }
                            )
                        }

                        composable(Routes.TRANSACTIONS) {
                            TransactionsScreen(
                                uiState = historyState,
                                onBack = { navController.popBackStack() },
                                onPeriodChange = salesHistoryViewModel::setHistoryPeriod,
                                onTypeFilterChange = salesHistoryViewModel::setHistoryTypeFilter,
                                onSearchChange = salesHistoryViewModel::setHistorySearch,
                                onCustomRangeSelected = salesHistoryViewModel::setCustomDateRange,
                                onUpdateTransaction = { tx, type, amt, note ->
                                    salesHistoryViewModel.updateTransaction(tx.id, type, amt, note, tx.timestamp) { res ->
                                        res.onSuccess { showMessage("লেনদেন আপডেট হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "আপডেট করা যায়নি") }
                                    }
                                },
                                onDeleteTransaction = { txId ->
                                    salesHistoryViewModel.deleteTransaction(txId) { res ->
                                        res.onSuccess { showMessage("লেনদেন মুছে ফেলা হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "মুছে ফেলা যায়নি") }
                                    }
                                }
                            )
                        }

                        composable(Routes.REPORTS) {
                            ReportsScreen(
                                uiState = reportsState,
                                onPeriodChange = reportsViewModel::setPeriod,
                                onExportPdfToUri = { uri ->
                                    reportsViewModel.exportPdfToUri(uri) { res ->
                                        res.onSuccess { showMessage("PDF রিপোর্ট সফলভাবে সেভ হয়েছে") }
                                            .onFailure { showMessage(it.message ?: "PDF তৈরি করা যায়নি") }
                                    }
                                },
                                onSharePdf = {
                                    reportsViewModel.createShareablePdfUri { res ->
                                        res.onSuccess { shareUri ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, "PDF রিপোর্ট শেয়ার করুন")
                                            )
                                        }.onFailure {
                                            showMessage(it.message ?: "PDF শেয়ার করা যায়নি")
                                        }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        CustomerFormDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showAddCustomerDialog = false },
            onSubmit = { name, phone, address, prevDue ->
                showAddCustomerDialog = false
                customersViewModel.addCustomer(name, phone, address, prevDue) { res ->
                    res.onSuccess { showMessage("নতুন কাস্টমার যোগ হয়েছে") }
                        .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                }
            }
        )
    }

    if (showCreditSaleDialog) {
        CreditSaleDialog(
            customers = customersState.allCustomers,
            preselectedCustomer = creditSalePreselected,
            currencySymbol = currencySymbol,
            onDismiss = { showCreditSaleDialog = false },
            onSubmit = { cid, purchase, paidNow, note ->
                showCreditSaleDialog = false
                customersViewModel.recordCreditPurchase(cid, purchase, paidNow, note) { res ->
                    res.onSuccess { showMessage("বাকি বিক্রি সংরক্ষিত হয়েছে") }
                        .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                }
            }
        )
    }

    if (showReceivePaymentDialog) {
        ReceivePaymentDialog(
            customers = customersState.allCustomers,
            preselectedCustomer = paymentPreselected,
            currencySymbol = currencySymbol,
            onDismiss = { showReceivePaymentDialog = false },
            onSubmit = { cid, amt, note ->
                showReceivePaymentDialog = false
                customersViewModel.recordPayment(cid, amt, note) { res ->
                    res.onSuccess { showMessage("টাকা জমা সংরক্ষিত হয়েছে") }
                        .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                }
            }
        )
    }

    if (showCashSaleDialog) {
        CashSaleDialog(
            customers = customersState.allCustomers,
            currencySymbol = currencySymbol,
            onDismiss = { showCashSaleDialog = false },
            onSubmit = { cid, amt, note ->
                showCashSaleDialog = false
                customersViewModel.recordCashSale(cid, amt, note) { res ->
                    res.onSuccess { showMessage("নগদ বিক্রি সংরক্ষিত হয়েছে") }
                        .onFailure { showMessage(it.message ?: "ত্রুটি হয়েছে") }
                }
            }
        )
    }
}
