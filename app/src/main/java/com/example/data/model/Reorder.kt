package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Reorder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val productId: String = "",
    val productName: String = "",
    val supplierName: String? = null,
    val quantity: Long = 1L,
    val unitCost: Double? = null,
    val totalCost: Double? = null,
    val status: String = "DRAFT", // DRAFT, ORDERED, RECEIVED, CANCELLED
    val orderDate: String? = null,
    val expectedDate: String? = null,
    val receivedDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "orderNumber" to orderNumber,
            "productId" to productId,
            "productName" to productName,
            "quantity" to quantity,
            "status" to status
        )
        supplierName?.let { map["supplierName"] = it }
        unitCost?.let { map["unitCost"] = it }
        totalCost?.let { map["totalCost"] = it }
        orderDate?.let { map["orderDate"] = it }
        expectedDate?.let { map["expectedDate"] = it }
        receivedDate?.let { map["receivedDate"] = it }
        return map
    }
}
