package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Reorder
import com.example.data.repository.StockLedgerRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiMessage {
    data class Success(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StockLedgerRepository(application)

    val products: StateFlow<List<Product>> = repository.observeProducts()
        .catch { e ->
            val firestoreError = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && e is CancellationException) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customers: StateFlow<List<Customer>> = repository.observeCustomers()
        .catch { e ->
            val firestoreError = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && e is CancellationException) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val invoices: StateFlow<List<Invoice>> = repository.observeInvoices()
        .catch { e ->
            val firestoreError = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && e is CancellationException) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val payments: StateFlow<List<Payment>> = repository.observePayments()
        .catch { e ->
            val firestoreError = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && e is CancellationException) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reorders: StateFlow<List<Reorder>> = repository.observeReorders()
        .catch { e ->
            val firestoreError = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && e is CancellationException) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _userMessage = MutableStateFlow<UiMessage?>(null)
    val userMessage: StateFlow<UiMessage?> = _userMessage.asStateFlow()

    fun clearMessage() {
        _userMessage.value = null
    }

    // =========================================================================
    // Product Actions
    // =========================================================================

    fun saveProduct(product: Product, isEditing: Boolean = false) {
        viewModelScope.launch {
            val result = if (isEditing) {
                repository.updateProduct(product)
            } else {
                repository.addProduct(product).map { Unit }
            }
            if (result.isSuccess) {
                _userMessage.value = UiMessage.Success(
                    if (isEditing) "Product updated" else "Product '${product.name}' added"
                )
            } else {
                _userMessage.value = UiMessage.Error("Failed to save product: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun adjustStock(productId: String, delta: Long) {
        viewModelScope.launch {
            val result = repository.adjustStock(productId, delta)
            if (result.isFailure) {
                _userMessage.value = UiMessage.Error("Failed to adjust stock: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            val result = repository.deleteProduct(productId)
            if (result.isSuccess) {
                _userMessage.value = UiMessage.Success("Product deleted")
            } else {
                _userMessage.value = UiMessage.Error("Failed to delete product")
            }
        }
    }

    // =========================================================================
    // Customer Actions
    // =========================================================================

    fun saveCustomer(customer: Customer, isEditing: Boolean = false) {
        viewModelScope.launch {
            val result = if (isEditing) {
                repository.updateCustomer(customer)
            } else {
                repository.addCustomer(customer).map { Unit }
            }
            if (result.isSuccess) {
                _userMessage.value = UiMessage.Success(
                    if (isEditing) "Customer updated" else "Customer '${customer.name}' registered"
                )
            } else {
                _userMessage.value = UiMessage.Error("Failed to save customer: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            val result = repository.deleteCustomer(customerId)
            if (result.isSuccess) {
                _userMessage.value = UiMessage.Success("Customer removed")
            } else {
                _userMessage.value = UiMessage.Error("Failed to delete customer")
            }
        }
    }

    // =========================================================================
    // Invoice Actions
    // =========================================================================

    fun createInvoice(
        invoice: Invoice,
        deductStock: Boolean = true,
        updateCustomerCredit: Boolean = true,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.createInvoice(invoice, deductStock, updateCustomerCredit)
            if (result.isSuccess) {
                val invId = result.getOrThrow()
                _userMessage.value = UiMessage.Success("Invoice ${invoice.invoiceNumber} created successfully")
                onSuccess(invId)
            } else {
                _userMessage.value = UiMessage.Error("Failed to create invoice: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateInvoiceStatus(invoiceId: String, newStatus: String) {
        viewModelScope.launch {
            val result = repository.updateInvoiceStatus(invoiceId, newStatus)
            if (result.isSuccess) {
                _userMessage.value = UiMessage.Success("Invoice status changed to $newStatus")
            } else {
                _userMessage.value = UiMessage.Error("Failed to update invoice status")
            }
        }
    }

    // =========================================================================
    // Payment Actions
    // =========================================================================

    fun recordPayment(payment: Payment) {
        viewModelScope.launch {
            val result = repository.recordPayment(payment)
            if (result.isSuccess) {
                _userMessage.value = UiMessage.Success("Payment of $${String.format(Locale.US, "%.2f", payment.amount)} recorded")
            } else {
                _userMessage.value = UiMessage.Error("Failed to record payment: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // =========================================================================
    // Reorders & Automated Purchase Orders
    // =========================================================================

    fun createReorder(product: Product, quantity: Long = product.reorderQuantity ?: 20L) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val orderNum = "PO-${System.currentTimeMillis().toString().takeLast(6)}"
            val reorder = Reorder(
                orderNumber = orderNum,
                productId = product.id,
                productName = product.name,
                supplierName = product.supplierName ?: "Default Supplier",
                quantity = quantity,
                unitCost = product.costPrice,
                totalCost = product.costPrice * quantity,
                status = "ORDERED",
                orderDate = today
            )
            val result = repository.createReorder(reorder)
            if (result.isSuccess) {
                _userMessage.value = UiMessage.Success("Reorder $orderNum placed for ${product.name}")
            } else {
                _userMessage.value = UiMessage.Error("Failed to create reorder")
            }
        }
    }

    fun autoReorderAllLowStock() {
        val lowStockItems = products.value.filter { it.isLowStock }
        if (lowStockItems.isEmpty()) {
            _userMessage.value = UiMessage.Success("All products are currently well-stocked!")
            return
        }
        viewModelScope.launch {
            var createdCount = 0
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            for (product in lowStockItems) {
                val qty = product.reorderQuantity ?: 20L
                val orderNum = "PO-${System.currentTimeMillis().toString().takeLast(6)}-${createdCount + 1}"
                val reorder = Reorder(
                    orderNumber = orderNum,
                    productId = product.id,
                    productName = product.name,
                    supplierName = product.supplierName ?: "Default Supplier",
                    quantity = qty,
                    unitCost = product.costPrice,
                    totalCost = product.costPrice * qty,
                    status = "ORDERED",
                    orderDate = today
                )
                if (repository.createReorder(reorder).isSuccess) {
                    createdCount++
                }
            }
            _userMessage.value = UiMessage.Success("Automated $createdCount restock purchase orders")
        }
    }

    fun receiveReorder(reorder: Reorder) {
        viewModelScope.launch {
            val result = repository.receiveReorder(reorder.id, reorder.productId, reorder.quantity)
            if (result.isSuccess) {
                _userMessage.value = UiMessage.Success("Stock received! Added ${reorder.quantity} units to inventory")
            } else {
                _userMessage.value = UiMessage.Error("Failed to receive stock: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // =========================================================================
    // Starter Business Data Seeder
    // =========================================================================

    fun seedSampleBusinessData() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val sampleProducts = listOf(
                Product(
                    name = "Wireless Barcode Scanner Pro",
                    sku = "SCAN-W100",
                    barcode = "890123456789",
                    category = "Electronics",
                    costPrice = 45.0,
                    sellingPrice = 89.99,
                    currentStock = 4, // Low stock!
                    minStockThreshold = 10,
                    reorderQuantity = 25,
                    supplierName = "TechSupply Global",
                    supplierContact = "orders@techsupply.com"
                ),
                Product(
                    name = "Thermal Receipt Paper (50pk)",
                    sku = "PAP-TH50",
                    barcode = "789012345678",
                    category = "Office Supplies",
                    costPrice = 12.0,
                    sellingPrice = 24.50,
                    currentStock = 85,
                    minStockThreshold = 20,
                    reorderQuantity = 50,
                    supplierName = "PaperWorks Ltd",
                    supplierContact = "sales@paperworks.com"
                ),
                Product(
                    name = "Heavy Duty Shipping Carton M",
                    sku = "BOX-MD10",
                    barcode = "678901234567",
                    category = "Packaging",
                    costPrice = 1.20,
                    sellingPrice = 2.99,
                    currentStock = 8, // Low stock!
                    minStockThreshold = 30,
                    reorderQuantity = 100,
                    supplierName = "BoxMart Distribution",
                    supplierContact = "supplies@boxmart.com"
                ),
                Product(
                    name = "Smart POS Terminal Touch",
                    sku = "POS-T800",
                    barcode = "567890123456",
                    category = "Electronics",
                    costPrice = 180.0,
                    sellingPrice = 349.00,
                    currentStock = 18,
                    minStockThreshold = 5,
                    reorderQuantity = 15,
                    supplierName = "TechSupply Global",
                    supplierContact = "orders@techsupply.com"
                )
            )

            for (p in sampleProducts) {
                repository.addProduct(p)
            }

            val sampleCustomers = listOf(
                Customer(
                    name = "Apex Retailers Co.",
                    company = "Apex Retail Group",
                    email = "accounts@apexretail.com",
                    phone = "+1 555-0192",
                    address = "742 Evergreen Terrace, Springfield",
                    creditLimit = 5000.0,
                    currentBalance = 1850.0
                ),
                Customer(
                    name = "Metro Supermarket",
                    company = "Metro Logistics",
                    email = "billing@metrostores.com",
                    phone = "+1 555-4821",
                    address = "1200 Market Street, Suite 400",
                    creditLimit = 10000.0,
                    currentBalance = 8400.0 // Near credit limit!
                ),
                Customer(
                    name = "Nexus Tech Hub",
                    company = "Nexus Innovation",
                    email = "nexus@nexustech.org",
                    phone = "+1 555-9032",
                    address = "350 Silicon Blvd",
                    creditLimit = 3000.0,
                    currentBalance = 450.0
                )
            )

            val createdCustIds = mutableListOf<Pair<String, Customer>>()
            for (c in sampleCustomers) {
                val res = repository.addCustomer(c)
                if (res.isSuccess) {
                    createdCustIds.add(res.getOrThrow() to c)
                }
            }

            // Sample Invoice
            if (createdCustIds.isNotEmpty()) {
                val (custId, cust) = createdCustIds[0]
                val invoice = Invoice(
                    invoiceNumber = "INV-2026-001",
                    customerId = custId,
                    customerName = cust.name,
                    items = listOf(
                        InvoiceItem("sample", "Wireless Barcode Scanner Pro", "SCAN-W100", 2, 89.99, 179.98),
                        InvoiceItem("sample2", "Thermal Receipt Paper (50pk)", "PAP-TH50", 4, 24.50, 98.00)
                    ),
                    subtotal = 277.98,
                    taxAmount = 22.24,
                    discountAmount = 0.0,
                    totalAmount = 300.22,
                    amountPaid = 100.0,
                    balanceDue = 200.22,
                    status = "PARTIALLY_PAID",
                    issueDate = today,
                    dueDate = today,
                    notes = "Net 30 payment terms"
                )
                repository.createInvoice(invoice, deductInventory = false, updateCustomerCredit = false)
            }

            _userMessage.value = UiMessage.Success("Demo business records loaded with products, clients & invoices!")
        }
    }
}
