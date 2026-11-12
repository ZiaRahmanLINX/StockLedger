package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class InvoiceItem(
    val productId: String = "",
    val productName: String = "",
    val sku: String = "",
    val quantity: Long = 1L,
    val unitPrice: Double = 0.0,
    val lineTotal: Double = 0.0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "productId" to productId,
            "productName" to productName,
            "sku" to sku,
            "quantity" to quantity,
            "unitPrice" to unitPrice,
            "lineTotal" to lineTotal
        )
    }
}

@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val items: List<InvoiceItem> = emptyList(),
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val status: String = "UNPAID", // UNPAID, PARTIALLY_PAID, PAID, CANCELLED
    val issueDate: String? = null,
    val dueDate: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isOverdue: Boolean
        get() = status != "PAID" && status != "CANCELLED" && dueDate != null

    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "invoiceNumber" to invoiceNumber,
            "customerId" to customerId,
            "customerName" to customerName,
            "items" to items.map { it.toMap() },
            "subtotal" to subtotal,
            "taxAmount" to taxAmount,
            "discountAmount" to discountAmount,
            "totalAmount" to totalAmount,
            "amountPaid" to amountPaid,
            "balanceDue" to balanceDue,
            "status" to status
        )
        issueDate?.let { map["issueDate"] = it }
        dueDate?.let { map["dueDate"] = it }
        notes?.let { map["notes"] = it }
        return map
    }
}
