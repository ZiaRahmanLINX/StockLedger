package com.example.ui.inventory

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
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.util.QrCodeGenerator
import java.util.Locale

@Composable
fun InventoryScreen(
    products: List<Product>,
    onSaveProduct: (Product, Boolean) -> Unit,
    onAdjustStock: (String, Long) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onReorderProduct: (Product) -> Unit,
    onOpenQrScanner: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LOW_STOCK, or Category
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var qrDialogProduct by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val categories = remember(products) {
        products.mapNotNull { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val filteredProducts = products.filter { product ->
        val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.sku.contains(searchQuery, ignoreCase = true) ||
                (product.barcode?.contains(searchQuery, ignoreCase = true) == true)

        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "LOW_STOCK" -> product.isLowStock
            else -> product.category == selectedFilter
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

            // Search Bar & Scan Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inventory_search_field"),
                    placeholder = { Text("Search by name, SKU, or barcode...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onOpenQrScanner,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("scan_qr_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR or Barcode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${products.size})") },
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }
                item {
                    val lowCount = products.count { it.isLowStock }
                    FilterChip(
                        selected = selectedFilter == "LOW_STOCK",
                        onClick = { selectedFilter = "LOW_STOCK" },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (lowCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDC2626))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Low Stock ($lowCount)")
                            }
                        },
                        modifier = Modifier.testTag("filter_chip_low_stock")
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedFilter == category,
                        onClick = { selectedFilter = category },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedFilter != "ALL") "No products match your filter" else "No inventory items yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { isAddDialogOpen = true },
                            modifier = Modifier.testTag("add_first_product_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Product")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onAdjustStock = { delta -> onAdjustStock(product.id, delta) },
                            onShowQr = { qrDialogProduct = product },
                            onEdit = { productToEdit = product },
                            onDelete = { productToDelete = product },
                            onReorder = { onReorderProduct(product) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
                    }
                }
            }
        }

        // Floating Action Button to Add Product
        FloatingActionButton(
            onClick = { isAddDialogOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_product_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    // Add Product Dialog
    if (isAddDialogOpen) {
        ProductFormDialog(
            product = null,
            onDismiss = { isAddDialogOpen = false },
            onConfirm = { product ->
                onSaveProduct(product, false)
                isAddDialogOpen = false
            }
        )
    }

    // Edit Product Dialog
    productToEdit?.let { product ->
        ProductFormDialog(
            product = product,
            onDismiss = { productToEdit = null },
            onConfirm = { updated ->
                onSaveProduct(updated, true)
                productToEdit = null
            }
        )
    }

    // QR Code Display Dialog
    qrDialogProduct?.let { product ->
        ProductQrDialog(
            product = product,
            onDismiss = { qrDialogProduct = null }
        )
    }

    // Delete confirmation dialog
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete '${product.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(product.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onAdjustStock: (Long) -> Unit,
    onShowQr: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReorder: () -> Unit
) {
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
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SKU: ${product.sku}${if (!product.barcode.isNullOrBlank()) " • Barcode: ${product.barcode}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Low Stock Badge
                if (product.currentStock <= 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("OUT OF STOCK", color = Color(0xFFDC2626), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (product.isLowStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("LOW STOCK", color = Color(0xFFD97706), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cost: $${String.format(Locale.US, "%.2f", product.costPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!product.category.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(product.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock Controls & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Counter with +/-
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { onAdjustStock(-1) },
                        modifier = Modifier.size(28.dp),
                        enabled = product.currentStock > 0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease stock", modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "${product.currentStock} in stock",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = if (product.isLowStock) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { onAdjustStock(1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase stock", modifier = Modifier.size(16.dp))
                    }
                }

                // Actions: QR, Reorder, Edit, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShowQr,
                        modifier = Modifier.size(34.dp).testTag("view_qr_${product.id}")
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Show QR Code", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (product.isLowStock) {
                        IconButton(
                            onClick = onReorder,
                            modifier = Modifier.size(34.dp).testTag("reorder_icon_${product.id}")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = "Reorder product", tint = Color(0xFFD97706))
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit product", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete product", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var sku by remember { mutableStateOf(product?.sku ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toString() ?: "0.0") }
    var sellingPrice by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "0.0") }
    var currentStock by remember { mutableStateOf(product?.currentStock?.toString() ?: "0") }
    var minThreshold by remember { mutableStateOf(product?.minStockThreshold?.toString() ?: "5") }
    var reorderQty by remember { mutableStateOf(product?.reorderQuantity?.toString() ?: "20") }
    var supplierName by remember { mutableStateOf(product?.supplierName ?: "") }
    var supplierContact by remember { mutableStateOf(product?.supplierContact ?: "") }

    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add Product" else "Edit Product") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    if (validationError != null) {
                        Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU *") },
                            modifier = Modifier.weight(1f).testTag("product_sku_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode / QR") },
                            modifier = Modifier.weight(1f).testTag("product_barcode_input"),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("Cost Price ($)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("Selling Price ($) *") },
                            modifier = Modifier.weight(1f).testTag("product_price_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentStock,
                            onValueChange = { currentStock = it },
                            label = { Text("Current Stock *") },
                            modifier = Modifier.weight(1f).testTag("product_stock_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minThreshold,
                            onValueChange = { minThreshold = it },
                            label = { Text("Min Alert Level") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = reorderQty,
                        onValueChange = { reorderQty = it },
                        label = { Text("Auto Reorder Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Supplier Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = supplierContact,
                        onValueChange = { supplierContact = it },
                        label = { Text("Supplier Contact / Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || sku.isBlank()) {
                        validationError = "Product Name and SKU are required"
                        return@Button
                    }
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val selling = sellingPrice.toDoubleOrNull() ?: 0.0
                    val stock = currentStock.toLongOrNull() ?: 0L
                    val minAlert = minThreshold.toLongOrNull() ?: 5L
                    val reorder = reorderQty.toLongOrNull() ?: 20L

                    val target = (product ?: Product()).copy(
                        name = name.trim(),
                        sku = sku.trim().uppercase(),
                        barcode = barcode.trim().ifBlank { null },
                        category = category.trim().ifBlank { null },
                        costPrice = cost,
                        sellingPrice = selling,
                        currentStock = stock,
                        minStockThreshold = minAlert,
                        reorderQuantity = reorder,
                        supplierName = supplierName.trim().ifBlank { null },
                        supplierContact = supplierContact.trim().ifBlank { null }
                    )
                    onConfirm(target)
                },
                modifier = Modifier.testTag("submit_product_button")
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
fun ProductQrDialog(
    product: Product,
    onDismiss: () -> Unit
) {
    val qrContent = product.barcode ?: product.sku
    val qrBitmap: Bitmap? = remember(qrContent) {
        QrCodeGenerator.generateQrBitmap(qrContent, size = 512)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan with scanner to lookup product in inventory or invoices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code for ${product.name}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text("Could not generate QR code", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SKU: ${product.sku}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (!product.barcode.isNullOrBlank()) {
                    Text(
                        text = "Barcode: ${product.barcode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Price: $${String.format(Locale.US, "%.2f", product.sellingPrice)} • Stock: ${product.currentStock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
