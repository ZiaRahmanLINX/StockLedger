package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val company: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val creditLimit: Double = 0.0,
    val currentBalance: Double = 0.0,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val availableCredit: Double
        get() = (creditLimit - currentBalance).coerceAtLeast(0.0)

    val isCreditExceeded: Boolean
        get() = currentBalance > creditLimit

    val creditUtilizationPercent: Float
        get() = if (creditLimit > 0.0) ((currentBalance / creditLimit) * 100f).coerceIn(0.0, 100.0).toFloat() else 0f

    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "name" to name,
            "creditLimit" to creditLimit,
            "currentBalance" to currentBalance
        )
        company?.let { map["company"] = it }
        email?.let { map["email"] = it }
        phone?.let { map["phone"] = it }
        address?.let { map["address"] = it }
        notes?.let { map["notes"] = it }
        return map
    }
}
