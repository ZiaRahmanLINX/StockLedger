package com.example.ui.invoices

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.util.QrCodeGenerator
import com.example.ui.dashboard.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicesScreen(
    invoices: List<Invoice>,
    customers: List<Customer>,
    products: List<Product>,
    onCreateInvoice: (Invoice, Boolean, Boolean) -> Unit,
    onUpdateInvoiceStatus: (String, String) -> Unit,
    onRecordPayment: (Payment) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, UNPAID, PAID
    var searchQuery by remember { mutableStateOf("") }
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var selectedInvoiceForDetail by remember { mutableStateOf<Invoice?>(null) }
    var invoiceForPayment by remember { mutableStateOf<Invoice?>(null) }

    val filteredInvoices = invoices.filter { inv ->
        val matchesSearch = searchQuery.isBlank() ||
                inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.customerName.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "UNPAID" -> inv.status == "UNPAID" || inv.status == "PARTIALLY_PAID"
            "PAID" -> inv.status == "PAID"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoices_search_input"),
                placeholder = { Text("Search by invoice # or client name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${invoices.size})") },
                    modifier = Modifier.testTag("invoices_filter_all")
                )
                val unpaidCount = invoices.count { it.status == "UNPAID" || it.status == "PARTIALLY_PAID" }
                FilterChip(
                    selected = selectedFilter == "UNPAID",
                    onClick = { selectedFilter = "UNPAID" },
                    label = { Text("Unpaid / Due ($unpaidCount)") },
                    modifier = Modifier.testTag("invoices_filter_unpaid")
                )
                val paidCount = invoices.count { it.status == "PAID" }
                FilterChip(
                    selected = selectedFilter == "PAID",
                    onClick = { selectedFilter = "PAID" },
                    label = { Text("Paid ($paidCount)") },
                    modifier = Modifier.testTag("invoices_filter_paid")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (invoices.isEmpty()) "No invoices issued yet" else "No matching invoices",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { isCreateDialogOpen = true },
                            modifier = Modifier.testTag("create_first_invoice_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Bill / Invoice")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            onClick = { selectedInvoiceForDetail = invoice },
                            onRecordPayment = { invoiceForPayment = invoice }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // FAB to create invoice
        FloatingActionButton(
            onClick = { isCreateDialogOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_invoice_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Invoice")
        }
    }

    // Create Invoice Dialog
    if (isCreateDialogOpen) {
        CreateInvoiceDialog(
            customers = customers,
            products = products,
            onDismiss = { isCreateDialogOpen = false },
            onConfirm = { invoice, deductStock, updateCredit ->
                onCreateInvoice(invoice, deductStock, updateCredit)
                isCreateDialogOpen = false
            }
        )
    }

    // Invoice Detail & Formatted Bill Preview Dialog
    selectedInvoiceForDetail?.let { inv ->
        InvoiceDetailDialog(
            invoice = inv,
            onDismiss = { selectedInvoiceForDetail = null },
            onRecordPayment = {
                selectedInvoiceForDetail = null
                invoiceForPayment = inv
            },
            onUpdateStatus = { newStatus ->
                onUpdateInvoiceStatus(inv.id, newStatus)
                selectedInvoiceForDetail = null
            }
        )
    }

    // Record Payment Dialog
    invoiceForPayment?.let { inv ->
        RecordPaymentDialog(
            invoice = inv,
            onDismiss = { invoiceForPayment = null },
            onConfirm = { payment ->
                onRecordPayment(payment)
                invoiceForPayment = null
            }
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onClick: () -> Unit,
    onRecordPayment: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = invoice.customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(invoice.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total: $${String.format(Locale.US, "%.2f", invoice.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (invoice.balanceDue > 0) {
                        Text(
                            text = "Balance Due: $${String.format(Locale.US, "%.2f", invoice.balanceDue)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                if (invoice.balanceDue > 0 && invoice.status != "CANCELLED") {
                    Button(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("pay_invoice_btn_${invoice.id}")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay", fontSize = 12.sp)
                    }
                }
            }

            if (!invoice.issueDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Issued: ${invoice.issueDate}${if (!invoice.dueDate.isNullOrBlank()) " • Due: ${invoice.dueDate}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CreateInvoiceDialog(
    customers: List<Customer>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (Invoice, Boolean, Boolean) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(customers.firstOrNull()) }
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    val lineItems = remember { mutableStateListOf<InvoiceItem>() }
    var taxRate by remember { mutableStateOf("0.0") }
    var discountAmountInput by remember { mutableStateOf("0.0") }
    var initialPaidInput by remember { mutableStateOf("0.0") }
    var notes by remember { mutableStateOf("") }
    var deductStock by remember { mutableStateOf(true) }
    var updateCredit by remember { mutableStateOf(true) }

    // Line item input states
    var selectedProductForLine by remember { mutableStateOf<Product?>(products.firstOrNull()) }
    var lineQuantityInput by remember { mutableStateOf("1") }
    var isProductDropdownExpanded by remember { mutableStateOf(false) }

    val subtotal = lineItems.sumOf { it.lineTotal }
    val tax = (subtotal * (taxRate.toDoubleOrNull() ?: 0.0) / 100.0)
    val discount = (discountAmountInput.toDoubleOrNull() ?: 0.0)
    val total = (subtotal + tax - discount).coerceAtLeast(0.0)
    val initialPaid = (initialPaidInput.toDoubleOrNull() ?: 0.0).coerceAtMost(total)
    val balanceDue = (total - initialPaid).coerceAtLeast(0.0)

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val invoiceNumber = remember { "INV-${System.currentTimeMillis().toString().takeLast(6)}" }

    // Credit limit check
    val isCreditWarning = selectedCustomer != null && selectedCustomer!!.creditLimit > 0 &&
            (selectedCustomer!!.currentBalance + balanceDue) > selectedCustomer!!.creditLimit

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Create Client Invoice / Bill", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(invoiceNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Client Picker
                    item {
                        Text("Select Client / Customer *", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { isCustomerDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("invoice_customer_selector"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedCustomer?.name ?: "Select Customer",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = isCustomerDropdownExpanded,
                                onDismissRequest = { isCustomerDropdownExpanded = false }
                            ) {
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(cust.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Credit Limit: $${String.format(Locale.US, "%.2f", cust.creditLimit)} • Bal: $${String.format(Locale.US, "%.2f", cust.currentBalance)}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCustomer = cust
                                            isCustomerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Client Credit Warning
                        if (isCreditWarning) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Credit Limit Exceeded! Limit is $${String.format(Locale.US, "%.2f", selectedCustomer!!.creditLimit)}, projected balance: $${String.format(Locale.US, "%.2f", selectedCustomer!!.currentBalance + balanceDue)}",
                                        color = Color(0xFF991B1B),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Add Line Item Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Add Product Line Item", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                // Product selector
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { isProductDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedProductForLine?.let { "${it.name} ($${it.sellingPrice})" } ?: "Select Product",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isProductDropdownExpanded,
                                        onDismissRequest = { isProductDropdownExpanded = false }
                                    ) {
                                        products.forEach { prod ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(prod.name, fontWeight = FontWeight.SemiBold)
                                                        Text("Stock: ${prod.currentStock} • Price: $${prod.sellingPrice}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                },
                                                onClick = {
                                                    selectedProductForLine = prod
                                                    isProductDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = lineQuantityInput,
                                        onValueChange = { lineQuantityInput = it },
                                        label = { Text("Qty") },
                                        modifier = Modifier.weight(1f).testTag("line_item_qty_input"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            val prod = selectedProductForLine ?: return@Button
                                            val qty = lineQuantityInput.toLongOrNull() ?: 1L
                                            if (qty <= 0) return@Button
                                            val itemTotal = prod.sellingPrice * qty
                                            lineItems.add(
                                                InvoiceItem(
                                                    productId = prod.id,
                                                    productName = prod.name,
                                                    sku = prod.sku,
                                                    quantity = qty,
                                                    unitPrice = prod.sellingPrice,
                                                    lineTotal = itemTotal
                                                )
                                            )
                                            lineQuantityInput = "1"
                                        },
                                        modifier = Modifier.weight(1f).testTag("add_line_item_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Item")
                                    }
                                }
                            }
                        }
                    }

                    // Line Items Table
                    item {
                        Text("Invoice Items (${lineItems.size})", fontWeight = FontWeight.Bold)
                    }

                    items(lineItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${item.quantity} x $${String.format(Locale.US, "%.2f", item.unitPrice)}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                "$${String.format(Locale.US, "%.2f", item.lineTotal)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { lineItems.remove(item) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Financial calculations
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = taxRate,
                                        onValueChange = { taxRate = it },
                                        label = { Text("Tax %") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = discountAmountInput,
                                        onValueChange = { discountAmountInput = it },
                                        label = { Text("Discount ($)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = initialPaidInput,
                                    onValueChange = { initialPaidInput = it },
                                    label = { Text("Amount Paid Now ($)") },
                                    modifier = Modifier.fillMaxWidth().testTag("invoice_initial_paid_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal:", color = Color.Gray)
                                    Text("$${String.format(Locale.US, "%.2f", subtotal)}")
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tax (${taxRate}%):", color = Color.Gray)
                                    Text("$${String.format(Locale.US, "%.2f", tax)}")
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount:", color = Color.Gray)
                                    Text("-$${String.format(Locale.US, "%.2f", discount)}")
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("$${String.format(Locale.US, "%.2f", total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Balance Due:", fontWeight = FontWeight.Bold, color = if (balanceDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                                    Text("$${String.format(Locale.US, "%.2f", balanceDue)}", fontWeight = FontWeight.Bold, color = if (balanceDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                                }
                            }
                        }
                    }

                    // Toggles for automated stock deduction & customer balance
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = deductStock, onCheckedChange = { deductStock = it })
                            Text("Automatically deduct product quantities from stock inventory", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = updateCredit, onCheckedChange = { updateCredit = it })
                            Text("Add balance due to client's outstanding credit ledger", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val cust = selectedCustomer ?: return@Button
                            if (lineItems.isEmpty()) return@Button
                            val status = if (balanceDue <= 0.0) "PAID" else if (initialPaid > 0) "PARTIALLY_PAID" else "UNPAID"

                            val invoice = Invoice(
                                invoiceNumber = invoiceNumber,
                                customerId = cust.id,
                                customerName = cust.name,
                                items = lineItems.toList(),
                                subtotal = subtotal,
                                taxAmount = tax,
                                discountAmount = discount,
                                totalAmount = total,
                                amountPaid = initialPaid,
                                balanceDue = balanceDue,
                                status = status,
                                issueDate = today,
                                dueDate = today,
                                notes = notes.ifBlank { null }
                            )
                            onConfirm(invoice, deductStock, updateCredit)
                        },
                        enabled = selectedCustomer != null && lineItems.isNotEmpty(),
                        modifier = Modifier.weight(1f).testTag("submit_create_invoice_btn")
                    ) {
                        Text("Create Invoice")
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceDetailDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onRecordPayment: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val qrBitmap: Bitmap? = remember(invoice.id) {
        QrCodeGenerator.generateQrBitmap("INVOICE:${invoice.invoiceNumber}:TOTAL:${invoice.totalAmount}:DUE:${invoice.balanceDue}", size = 320)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bill Preview", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(invoice.status)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Printable / Formatted Bill Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            // Invoice Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("StockLedger Invoicing", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Invoice #${invoice.invoiceNumber}", fontWeight = FontWeight.Bold)
                                    Text("Date: ${invoice.issueDate ?: "N/A"}", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "Invoice QR",
                                        modifier = Modifier.size(60.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                        }

                        item {
                            // Bill To
                            Text("Billed To:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(invoice.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        item {
                            Text("Line Items:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        items(invoice.items) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.productName} (x${item.quantity})", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text("$${String.format(Locale.US, "%.2f", item.lineTotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", color = Color.Gray, fontSize = 12.sp)
                                Text("$${String.format(Locale.US, "%.2f", invoice.subtotal)}", fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tax:", color = Color.Gray, fontSize = 12.sp)
                                Text("$${String.format(Locale.US, "%.2f", invoice.taxAmount)}", fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount:", color = Color.Gray, fontSize = 12.sp)
                                Text("-$${String.format(Locale.US, "%.2f", invoice.discountAmount)}", fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("$${String.format(Locale.US, "%.2f", invoice.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount Paid:", color = Color(0xFF15803D), fontSize = 13.sp)
                                Text("$${String.format(Locale.US, "%.2f", invoice.amountPaid)}", color = Color(0xFF15803D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Balance Due:", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 14.sp)
                                Text("$${String.format(Locale.US, "%.2f", invoice.balanceDue)}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Share Bill, Record Payment, Mark Paid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = "INVOICE ${invoice.invoiceNumber}\nClient: ${invoice.customerName}\nTotal: $${invoice.totalAmount}\nBalance Due: $${invoice.balanceDue}\nStatus: ${invoice.status}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoice.invoiceNumber}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Invoice Bill"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    if (invoice.balanceDue > 0) {
                        Button(
                            onClick = onRecordPayment,
                            modifier = Modifier.weight(1f).testTag("record_payment_from_detail_btn")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record Pay")
                        }
                    } else if (invoice.status != "PAID") {
                        Button(
                            onClick = { onUpdateStatus("PAID") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark as Paid")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onConfirm: (Payment) -> Unit
) {
    var amountText by remember { mutableStateOf(invoice.balanceDue.toString()) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val methods = listOf("Cash", "Bank Transfer", "Credit Card", "Check", "Mobile Money")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for ${invoice.invoiceNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Client: ${invoice.customerName} • Outstanding: $${String.format(Locale.US, "%.2f", invoice.balanceDue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($) *") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(methods) { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference # / Receipt Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    if (amount <= 0) return@Button
                    val payment = Payment(
                        customerId = invoice.customerId,
                        customerName = invoice.customerName,
                        invoiceId = invoice.id,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        referenceNote = reference.ifBlank { null },
                        paymentDate = today
                    )
                    onConfirm(payment)
                },
                modifier = Modifier.testTag("submit_record_payment_btn")
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
