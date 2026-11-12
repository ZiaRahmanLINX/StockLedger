package com.example.ui.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.data.model.Reorder
import java.util.Locale

@Composable
fun DashboardScreen(
    products: List<Product>,
    customers: List<Customer>,
    invoices: List<Invoice>,
    reorders: List<Reorder>,
    onNavigateToInventory: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToReorders: () -> Unit,
    onOpenAddProduct: () -> Unit,
    onOpenNewInvoice: () -> Unit,
    onOpenAddCustomer: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onReorderProduct: (Product) -> Unit,
    onAutoReorderAll: () -> Unit,
    onSeedData: () -> Unit
) {
    val lowStockProducts = products.filter { it.isLowStock }
    val totalInventoryCost = products.sumOf { it.stockValueCost }
    val totalInventoryRetail = products.sumOf { it.stockValueRetail }
    val totalReceivables = customers.sumOf { it.currentBalance }
    val totalInvoiced = invoices.sumOf { it.totalAmount }
    val creditAlertCustomers = customers.filter { it.creditLimit > 0 && it.currentBalance >= it.creditLimit * 0.8 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Business Overview",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Live inventory, billing & customer ledger",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (products.isEmpty() && customers.isEmpty()) {
                    OutlinedButton(
                        onClick = onSeedData,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("load_demo_data_button")
                    ) {
                        Text("Load Demo Data", fontSize = 12.sp)
                    }
                }
            }
        }

        // Metrics Grid (2x2 Cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Inventory Value",
                    value = "$${String.format(Locale.US, "%,.2f", totalInventoryCost)}",
                    subtitle = "Retail: $${String.format(Locale.US, "%,.2f", totalInventoryRetail)}",
                    icon = Icons.Default.Inventory2,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToInventory
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Receivables (Credit)",
                    value = "$${String.format(Locale.US, "%,.2f", totalReceivables)}",
                    subtitle = "${customers.size} clients registered",
                    icon = Icons.Default.CreditScore,
                    containerColor = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    onClick = onNavigateToCustomers
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Sales",
                    value = "$${String.format(Locale.US, "%,.2f", totalInvoiced)}",
                    subtitle = "${invoices.size} invoices issued",
                    icon = Icons.Default.Receipt,
                    containerColor = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0284C7),
                    onClick = onNavigateToInvoices
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Low Stock Items",
                    value = "${lowStockProducts.size}",
                    subtitle = if (lowStockProducts.isEmpty()) "Stock healthy" else "Action required",
                    icon = Icons.Default.Warning,
                    containerColor = if (lowStockProducts.isEmpty()) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    iconTint = if (lowStockProducts.isEmpty()) Color(0xFF16A34A) else Color(0xFFDC2626),
                    onClick = onNavigateToInventory
                )
            }
        }

        // Quick Actions
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    QuickActionButton(
                        icon = Icons.Default.Receipt,
                        label = "New Invoice",
                        testTag = "quick_new_invoice_btn",
                        onClick = onOpenNewInvoice
                    )
                }
                item {
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        label = "Add Product",
                        testTag = "quick_add_product_btn",
                        onClick = onOpenAddProduct
                    )
                }
                item {
                    QuickActionButton(
                        icon = Icons.Default.QrCodeScanner,
                        label = "Scan QR / Code",
                        testTag = "quick_scan_qr_btn",
                        onClick = onOpenQrScanner
                    )
                }
                item {
                    QuickActionButton(
                        icon = Icons.Default.PersonAdd,
                        label = "Add Customer",
                        testTag = "quick_add_customer_btn",
                        onClick = onOpenAddCustomer
                    )
                }
            }
        }

        // Low Stock Alert Banner & Automated Reorder Trigger
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFCA5A5))),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${lowStockProducts.size} Items Below Threshold",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "Automate reorder purchase orders",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }

                            Button(
                                onClick = onAutoReorderAll,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("auto_reorder_all_btn")
                            ) {
                                Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Auto Reorder", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lowStockProducts.take(3).forEach { product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "Stock: ${product.currentStock} / Min: ${product.minStockThreshold} • SKU: ${product.sku}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { onReorderProduct(product) },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("reorder_product_${product.id}")
                                    ) {
                                        Text("Reorder +${product.reorderQuantity ?: 20}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Credit Risk / Limit Warning Section
        if (creditAlertCustomers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditScore,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Customer Credit Warning (${creditAlertCustomers.size} Near / At Limit)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        creditAlertCustomers.take(2).forEach { cust ->
                            Text(
                                text = "• ${cust.name}: $${String.format(Locale.US, "%.2f", cust.currentBalance)} used of $${String.format(Locale.US, "%.2f", cust.creditLimit)} limit (${cust.creditUtilizationPercent.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }
        }

        // Recent Invoices Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Invoices",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View All (${invoices.size})",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToInvoices() }
                )
            }
        }

        if (invoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No invoices created yet. Tap '+ New Invoice' to bill a client.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(invoices.take(3), key = { it.id }) { invoice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToInvoices() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = invoice.invoiceNumber,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${invoice.customerName} • ${invoice.items.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", invoice.totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusBadge(invoice.status)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "PAID" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "PARTIALLY_PAID" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        "CANCELLED" -> Color(0xFFF1F5F9) to Color(0xFF64748B)
        else -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.replace("_", " "),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = iconTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
