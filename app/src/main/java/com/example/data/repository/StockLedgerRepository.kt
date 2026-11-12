package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Reorder
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StockLedgerRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // =========================================================================
    // Products
    // =========================================================================

    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/products"
        emitAll(
            db.collection("users").document(uid).collection("products")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Product::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addProduct(product: Product): Result<String> {
        return try {
            val uid = requireUserId()
            val collection = db.collection("users").document(uid).collection("products")
            val docId = if (product.id.isNotBlank()) product.id else "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val docRef = collection.document(docId)

            val payload = product.copy(userId = uid, id = docId).toFirestoreMap().toMutableMap()
            payload["userId"] = uid
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["updatedAt"] = FieldValue.serverTimestamp()

            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/products")
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(product.id.isNotBlank()) { "Product ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("products").document(product.id)

            val payload = product.copy(userId = uid).toFirestoreMap().toMutableMap()
            payload["updatedAt"] = FieldValue.serverTimestamp()

            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/products/${product.id}")
            Result.failure(e)
        }
    }

    suspend fun adjustStock(productId: String, delta: Long): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("products").document(productId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val current = snapshot.getLong("currentStock") ?: 0L
                val newStock = (current + delta).coerceAtLeast(0L)
                transaction.update(docRef, mapOf(
                    "currentStock" to newStock,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/products/$productId")
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("products").document(productId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/products/$productId")
            Result.failure(e)
        }
    }

    // =========================================================================
    // Customers
    // =========================================================================

    fun observeCustomers(): Flow<List<Customer>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/customers"
        emitAll(
            db.collection("users").document(uid).collection("customers")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Customer::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addCustomer(customer: Customer): Result<String> {
        return try {
            val uid = requireUserId()
            val collection = db.collection("users").document(uid).collection("customers")
            val docId = if (customer.id.isNotBlank()) customer.id else "cust_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val docRef = collection.document(docId)

            val payload = customer.copy(userId = uid, id = docId).toFirestoreMap().toMutableMap()
            payload["userId"] = uid
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["updatedAt"] = FieldValue.serverTimestamp()

            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/customers")
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(customer.id.isNotBlank()) { "Customer ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("customers").document(customer.id)

            val payload = customer.copy(userId = uid).toFirestoreMap().toMutableMap()
            payload["updatedAt"] = FieldValue.serverTimestamp()

            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/customers/${customer.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("customers").document(customerId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/customers/$customerId")
            Result.failure(e)
        }
    }

    // =========================================================================
    // Invoices
    // =========================================================================

    fun observeInvoices(): Flow<List<Invoice>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/invoices"
        emitAll(
            db.collection("users").document(uid).collection("invoices")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Invoice::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createInvoice(
        invoice: Invoice,
        deductInventory: Boolean = true,
        updateCustomerCredit: Boolean = true
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val invoiceId = if (invoice.id.isNotBlank()) invoice.id else "inv_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val invoiceDoc = db.collection("users").document(uid).collection("invoices").document(invoiceId)

            val payload = invoice.copy(userId = uid, id = invoiceId).toFirestoreMap().toMutableMap()
            payload["userId"] = uid
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["updatedAt"] = FieldValue.serverTimestamp()

            invoiceDoc.set(payload).await()

            // Deduct stock for line items if requested
            if (deductInventory) {
                for (item in invoice.items) {
                    if (item.productId.isNotBlank()) {
                        adjustStock(item.productId, -item.quantity)
                    }
                }
            }

            // Update customer balance if unpaid balance exists
            if (updateCustomerCredit && invoice.customerId.isNotBlank() && invoice.balanceDue > 0) {
                val custRef = db.collection("users").document(uid).collection("customers").document(invoice.customerId)
                try {
                    db.runTransaction { t ->
                        val snap = t.get(custRef)
                        if (snap.exists()) {
                            val currBal = snap.getDouble("currentBalance") ?: 0.0
                            t.update(custRef, mapOf(
                                "currentBalance" to currBal + invoice.balanceDue,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ))
                        }
                    }.await()
                } catch (e: Exception) {
                    // Log but don't fail invoice creation
                    handleFirestoreError(e, OperationType.UPDATE, custRef.path)
                }
            }

            Result.success(invoiceId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/invoices")
            Result.failure(e)
        }
    }

    suspend fun updateInvoiceStatus(invoiceId: String, newStatus: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("invoices").document(invoiceId)
            docRef.update(mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/invoices/$invoiceId")
            Result.failure(e)
        }
    }

    // =========================================================================
    // Payments
    // =========================================================================

    fun observePayments(): Flow<List<Payment>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/payments"
        emitAll(
            db.collection("users").document(uid).collection("payments")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Payment::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun recordPayment(payment: Payment): Result<String> {
        return try {
            val uid = requireUserId()
            val paymentId = if (payment.id.isNotBlank()) payment.id else "pay_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val paymentDoc = db.collection("users").document(uid).collection("payments").document(paymentId)

            val payload = payment.copy(userId = uid, id = paymentId).toFirestoreMap().toMutableMap()
            payload["userId"] = uid
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["updatedAt"] = FieldValue.serverTimestamp()

            paymentDoc.set(payload).await()

            // Update customer balance (decrement balance)
            if (payment.customerId.isNotBlank() && payment.amount > 0) {
                val custRef = db.collection("users").document(uid).collection("customers").document(payment.customerId)
                try {
                    db.runTransaction { t ->
                        val snap = t.get(custRef)
                        if (snap.exists()) {
                            val currBal = snap.getDouble("currentBalance") ?: 0.0
                            val newBal = (currBal - payment.amount).coerceAtLeast(0.0)
                            t.update(custRef, mapOf(
                                "currentBalance" to newBal,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ))
                        }
                    }.await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, custRef.path)
                }
            }

            // Update invoice amountPaid and balanceDue if linked to an invoice
            payment.invoiceId?.let { invId ->
                if (invId.isNotBlank()) {
                    val invRef = db.collection("users").document(uid).collection("invoices").document(invId)
                    try {
                        db.runTransaction { t ->
                            val snap = t.get(invRef)
                            if (snap.exists()) {
                                val total = snap.getDouble("totalAmount") ?: 0.0
                                val prevPaid = snap.getDouble("amountPaid") ?: 0.0
                                val newPaid = prevPaid + payment.amount
                                val newBalance = (total - newPaid).coerceAtLeast(0.0)
                                val status = if (newBalance <= 0.0) "PAID" else "PARTIALLY_PAID"
                                t.update(invRef, mapOf(
                                    "amountPaid" to newPaid,
                                    "balanceDue" to newBalance,
                                    "status" to status,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                ))
                            }
                        }.await()
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.UPDATE, invRef.path)
                    }
                }
            }

            Result.success(paymentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/payments")
            Result.failure(e)
        }
    }

    // =========================================================================
    // Automated & Manual Reorders
    // =========================================================================

    fun observeReorders(): Flow<List<Reorder>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/reorders"
        emitAll(
            db.collection("users").document(uid).collection("reorders")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Reorder::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createReorder(reorder: Reorder): Result<String> {
        return try {
            val uid = requireUserId()
            val reorderId = if (reorder.id.isNotBlank()) reorder.id else "po_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val docRef = db.collection("users").document(uid).collection("reorders").document(reorderId)

            val payload = reorder.copy(userId = uid, id = reorderId).toFirestoreMap().toMutableMap()
            payload["userId"] = uid
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["updatedAt"] = FieldValue.serverTimestamp()

            docRef.set(payload).await()
            Result.success(reorderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/reorders")
            Result.failure(e)
        }
    }

    suspend fun receiveReorder(reorderId: String, productId: String, quantity: Long): Result<Unit> {
        return try {
            val uid = requireUserId()
            val reorderDoc = db.collection("users").document(uid).collection("reorders").document(reorderId)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            reorderDoc.update(mapOf(
                "status" to "RECEIVED",
                "receivedDate" to today,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()

            // Increment product stock
            if (productId.isNotBlank() && quantity > 0) {
                adjustStock(productId, quantity)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/reorders/$reorderId")
            Result.failure(e)
        }
    }
}
