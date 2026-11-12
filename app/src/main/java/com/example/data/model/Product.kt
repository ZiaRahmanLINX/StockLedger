package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sku: String = "",
    val barcode: String? = null,
    val category: String? = null,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val currentStock: Long = 0L,
    val minStockThreshold: Long = 0L,
    val reorderQuantity: Long? = 10L,
    val supplierName: String? = null,
    val supplierContact: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isLowStock: Boolean
        get() = currentStock <= minStockThreshold

    val stockValueCost: Double
        get() = currentStock * costPrice

    val stockValueRetail: Double
        get() = currentStock * sellingPrice

    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "name" to name,
            "sku" to sku,
            "costPrice" to costPrice,
            "sellingPrice" to sellingPrice,
            "currentStock" to currentStock,
            "minStockThreshold" to minStockThreshold
        )
        barcode?.let { map["barcode"] = it }
        category?.let { map["category"] = it }
        reorderQuantity?.let { map["reorderQuantity"] = it }
        supplierName?.let { map["supplierName"] = it }
        supplierContact?.let { map["supplierContact"] = it }
        notes?.let { map["notes"] = it }
        return map
    }
}
