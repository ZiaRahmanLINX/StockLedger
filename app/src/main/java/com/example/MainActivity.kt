package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.customers.CustomerFormDialog
import com.example.ui.customers.CustomersScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.inventory.InventoryScreen
import com.example.ui.inventory.ProductFormDialog
import com.example.ui.inventory.QrScannerDialog
import com.example.ui.invoices.CreateInvoiceDialog
import com.example.ui.invoices.InvoicesScreen
import com.example.ui.reorder.ReordersScreen
import com.example.ui.theme.StockLedgerTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class AppScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    INVENTORY("Inventory", Icons.Default.Inventory),
    INVOICES("Invoices", Icons.Default.ReceiptLong),
    CUSTOMERS("Clients", Icons.Default.Group),
    REORDERS("Reorders", Icons.Default.ShoppingBag)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockLedgerTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    // Attempt silent auto-sign-in on app cold start
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            val credentialManager = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {
                    // Stay on AuthScreen
                },
                scope = coroutineScope
            )
        }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        MainFeatureContainer(
            currentUserId = currentUser!!.uid,
            userEmail = currentUser?.email ?: "User",
            onSignOutRequested = {
                val credentialManager = CredentialManager.create(context)
                signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {
                        currentUser = null
                    },
                    scope = coroutineScope
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureContainer(
    currentUserId: String,
    userEmail: String,
    onSignOutRequested: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val reorders by viewModel.reorders.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    var isSignOutConfirmOpen by remember { mutableStateOf(false) }
    var isGlobalScannerOpen by remember { mutableStateOf(false) }
    var isNewInvoiceDialogOpen by remember { mutableStateOf(false) }
    var isAddProductDialogOpen by remember { mutableStateOf(false) }
    var isAddCustomerDialogOpen by remember { mutableStateOf(false) }

    // Show Snackbar notifications
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(msg.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar(msg.message)
            }
            viewModel.clearMessage()
        }
    }

    // BackHandler: Navigate back to Dashboard before closing
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "StockLedger",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userEmail,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isGlobalScannerOpen = true },
                        modifier = Modifier.testTag("topbar_qr_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR or Barcode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { isSignOutConfirmOpen = true },
                        modifier = Modifier.testTag("topbar_signout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                AppScreen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        },
                        label = { Text(screen.title, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        products = products,
                        customers = customers,
                        invoices = invoices,
                        reorders = reorders,
                        onNavigateToInventory = { currentScreen = AppScreen.INVENTORY },
                        onNavigateToInvoices = { currentScreen = AppScreen.INVOICES },
                        onNavigateToCustomers = { currentScreen = AppScreen.CUSTOMERS },
                        onNavigateToReorders = { currentScreen = AppScreen.REORDERS },
                        onOpenAddProduct = { isAddProductDialogOpen = true },
                        onOpenNewInvoice = { isNewInvoiceDialogOpen = true },
                        onOpenAddCustomer = { isAddCustomerDialogOpen = true },
                        onOpenQrScanner = { isGlobalScannerOpen = true },
                        onReorderProduct = { prod -> viewModel.createReorder(prod) },
                        onAutoReorderAll = { viewModel.autoReorderAllLowStock() },
                        onSeedData = { viewModel.seedSampleBusinessData() }
                    )
                }

                AppScreen.INVENTORY -> {
                    InventoryScreen(
                        products = products,
                        onSaveProduct = { prod, isEdit -> viewModel.saveProduct(prod, isEdit) },
                        onAdjustStock = { id, delta -> viewModel.adjustStock(id, delta) },
                        onDeleteProduct = { id -> viewModel.deleteProduct(id) },
                        onReorderProduct = { prod -> viewModel.createReorder(prod) },
                        onOpenQrScanner = { isGlobalScannerOpen = true }
                    )
                }

                AppScreen.INVOICES -> {
                    InvoicesScreen(
                        invoices = invoices,
                        customers = customers,
                        products = products,
                        onCreateInvoice = { inv, deductStock, updateCredit ->
                            viewModel.createInvoice(inv, deductStock, updateCredit)
                        },
                        onUpdateInvoiceStatus = { id, status ->
                            viewModel.updateInvoiceStatus(id, status)
                        },
                        onRecordPayment = { payment ->
                            viewModel.recordPayment(payment)
                        }
                    )
                }

                AppScreen.CUSTOMERS -> {
                    CustomersScreen(
                        customers = customers,
                        payments = payments,
                        onSaveCustomer = { cust, isEdit -> viewModel.saveCustomer(cust, isEdit) },
                        onDeleteCustomer = { id -> viewModel.deleteCustomer(id) },
                        onRecordPayment = { payment -> viewModel.recordPayment(payment) }
                    )
                }

                AppScreen.REORDERS -> {
                    ReordersScreen(
                        reorders = reorders,
                        products = products,
                        onAutoReorderAll = { viewModel.autoReorderAllLowStock() },
                        onReceiveReorder = { reorder -> viewModel.receiveReorder(reorder) },
                        onManualReorder = { prod -> viewModel.createReorder(prod) }
                    )
                }
            }
        }
    }

    // Global QR / Barcode Scanner Modal
    if (isGlobalScannerOpen) {
        QrScannerDialog(
            products = products,
            onDismiss = { isGlobalScannerOpen = false },
            onProductSelected = { prod ->
                currentScreen = AppScreen.INVENTORY
            },
            onStockAdjusted = { prod, delta ->
                viewModel.adjustStock(prod.id, delta)
            }
        )
    }

    // Quick Add Product Dialog
    if (isAddProductDialogOpen) {
        ProductFormDialog(
            product = null,
            onDismiss = { isAddProductDialogOpen = false },
            onConfirm = { prod ->
                viewModel.saveProduct(prod, false)
                isAddProductDialogOpen = false
            }
        )
    }

    // Quick New Invoice Dialog
    if (isNewInvoiceDialogOpen) {
        CreateInvoiceDialog(
            customers = customers,
            products = products,
            onDismiss = { isNewInvoiceDialogOpen = false },
            onConfirm = { inv, deductStock, updateCredit ->
                viewModel.createInvoice(inv, deductStock, updateCredit)
                isNewInvoiceDialogOpen = false
                currentScreen = AppScreen.INVOICES
            }
        )
    }

    // Quick Add Customer Dialog
    if (isAddCustomerDialogOpen) {
        CustomerFormDialog(
            customer = null,
            onDismiss = { isAddCustomerDialogOpen = false },
            onConfirm = { cust ->
                viewModel.saveCustomer(cust, false)
                isAddCustomerDialogOpen = false
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (isSignOutConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isSignOutConfirmOpen = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of StockLedger?") },
            confirmButton = {
                Button(
                    onClick = {
                        isSignOutConfirmOpen = false
                        onSignOutRequested()
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSignOutConfirmOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
