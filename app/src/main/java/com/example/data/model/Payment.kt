package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String? = null,
    val invoiceId: String? = null,
    val amount: Double = 0.0,
    val paymentMethod: String = "Cash", // Cash, Bank Transfer, Card, Check
    val referenceNote: String? = null,
    val paymentDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "customerId" to customerId,
            "amount" to amount,
            "paymentMethod" to paymentMethod
        )
        customerName?.let { map["customerName"] = it }
        invoiceId?.let { map["invoiceId"] = it }
        referenceNote?.let { map["referenceNote"] = it }
        paymentDate?.let { map["paymentDate"] = it }
        return map
    }
}
