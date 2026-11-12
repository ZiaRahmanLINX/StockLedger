package com.example.ui.customers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Customer
import com.example.data.model.Payment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomersScreen(
    customers: List<Customer>,
    payments: List<Payment>,
    onSaveCustomer: (Customer, Boolean) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onRecordPayment: (Payment) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerForPayment by remember { mutableStateOf<Customer?>(null) }
    var customerForHistory by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = customers.filter { cust ->
        searchQuery.isBlank() ||
                cust.name.contains(searchQuery, ignoreCase = true) ||
                (cust.company?.contains(searchQuery, ignoreCase = true) == true) ||
                (cust.email?.contains(searchQuery, ignoreCase = true) == true) ||
                (cust.phone?.contains(searchQuery, ignoreCase = true) == true)
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
                    .testTag("customer_search_input"),
                placeholder = { Text("Search clients by name, company, email...") },
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

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (customers.isEmpty()) "No clients registered yet" else "No matching clients",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { isAddDialogOpen = true },
                            modifier = Modifier.testTag("add_first_customer_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Client Account")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onRecordPayment = { customerForPayment = customer },
                            onViewHistory = { customerForHistory = customer },
                            onEdit = { customerToEdit = customer },
                            onDelete = { customerToDelete = customer }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { isAddDialogOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_customer_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Customer")
        }
    }

    if (isAddDialogOpen) {
        CustomerFormDialog(
            customer = null,
            onDismiss = { isAddDialogOpen = false },
            onConfirm = { cust ->
                onSaveCustomer(cust, false)
                isAddDialogOpen = false
            }
        )
    }

    customerToEdit?.let { cust ->
        CustomerFormDialog(
            customer = cust,
            onDismiss = { customerToEdit = null },
            onConfirm = { updated ->
                onSaveCustomer(updated, true)
                customerToEdit = null
            }
        )
    }

    customerForPayment?.let { cust ->
        DirectCustomerPaymentDialog(
            customer = cust,
            onDismiss = { customerForPayment = null },
            onConfirm = { payment ->
                onRecordPayment(payment)
                customerForPayment = null
            }
        )
    }

    customerForHistory?.let { cust ->
        val customerPayments = payments.filter { it.customerId == cust.id }
        CustomerHistoryDialog(
            customer = cust,
            payments = customerPayments,
            onDismiss = { customerForHistory = null }
        )
    }

    customerToDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Client Account") },
            text = { Text("Are you sure you want to delete '${cust.name}'? Outstanding balance is $${String.format(Locale.US, "%.2f", cust.currentBalance)}.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(cust.id)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    onRecordPayment: () -> Unit,
    onViewHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (customer.creditUtilizationPercent / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        customer.isCreditExceeded -> Color(0xFFDC2626)
        customer.creditUtilizationPercent >= 80f -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!customer.company.isNullOrBlank()) {
                        Text(
                            text = customer.company,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (customer.isCreditExceeded) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("CREDIT EXCEEDED", color = Color(0xFFDC2626), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Credit Limit Bar & Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Outstanding: $${String.format(Locale.US, "%.2f", customer.currentBalance)}",
                    fontWeight = FontWeight.Bold,
                    color = if (customer.currentBalance > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Credit Limit: $${String.format(Locale.US, "%.2f", customer.creditLimit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Available: $${String.format(Locale.US, "%.2f", customer.availableCredit)}",
                    fontSize = 11.sp,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${customer.creditUtilizationPercent.toInt()}% utilized",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contact Info & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!customer.phone.isNullOrBlank()) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(customer.phone, fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (!customer.email.isNullOrBlank()) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(customer.email, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRecordPayment,
                        modifier = Modifier.size(32.dp).testTag("customer_pay_btn_${customer.id}")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = "Record Payment", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = onViewHistory,
                        modifier = Modifier.size(32.dp).testTag("customer_history_btn_${customer.id}")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Payment History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerFormDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onConfirm: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var company by remember { mutableStateOf(customer?.company ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var creditLimitText by remember { mutableStateOf(customer?.creditLimit?.toString() ?: "1000.0") }
    var balanceText by remember { mutableStateOf(customer?.currentBalance?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "Register Client Account" else "Edit Client Account") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (validationError != null) {
                    item {
                        Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Client / Customer Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company / Organization") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Billing Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = creditLimitText,
                            onValueChange = { creditLimitText = it },
                            label = { Text("Credit Limit ($) *") },
                            modifier = Modifier.weight(1f).testTag("customer_credit_limit_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = balanceText,
                            onValueChange = { balanceText = it },
                            label = { Text("Initial Balance ($)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Account Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Customer Name is required"
                        return@Button
                    }
                    val limit = creditLimitText.toDoubleOrNull() ?: 0.0
                    val balance = balanceText.toDoubleOrNull() ?: 0.0

                    val target = (customer ?: Customer()).copy(
                        name = name.trim(),
                        company = company.trim().ifBlank { null },
                        email = email.trim().ifBlank { null },
                        phone = phone.trim().ifBlank { null },
                        address = address.trim().ifBlank { null },
                        creditLimit = limit,
                        currentBalance = balance,
                        notes = notes.trim().ifBlank { null }
                    )
                    onConfirm(target)
                },
                modifier = Modifier.testTag("submit_customer_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DirectCustomerPaymentDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (Payment) -> Unit
) {
    var amountText by remember { mutableStateOf(customer.currentBalance.toString()) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val methods = listOf("Cash", "Bank Transfer", "Credit Card", "Check")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for ${customer.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current Outstanding: $${String.format(Locale.US, "%.2f", customer.currentBalance)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($) *") },
                    modifier = Modifier.fillMaxWidth().testTag("cust_payment_amount_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference / Receipt #") },
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
                        customerId = customer.id,
                        customerName = customer.name,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        referenceNote = reference.ifBlank { null },
                        paymentDate = today
                    )
                    onConfirm(payment)
                },
                modifier = Modifier.testTag("submit_cust_payment_btn")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CustomerHistoryDialog(
    customer: Customer,
    payments: List<Payment>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Payment History (${payments.size} records)", fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (payments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No recorded payments yet for this client.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(payments) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "$${String.format(Locale.US, "%.2f", p.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        "${p.paymentMethod}${if (!p.referenceNote.isNullOrBlank()) " • ${p.referenceNote}" else ""}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(p.paymentDate ?: "N/A", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}
