package com.rkdevstudios.voxly.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rkdevstudios.voxly.data.model.Transaction
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WalletRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : WalletRepository {

    override suspend fun requestWithdrawal(userId: String, amount: Double, method: String, details: String) {
        val userRef = firestore.collection("users").document(userId)
        val withdrawalsRef = firestore.collection("withdrawals").document()
        val transactionsRef = firestore.collection("transactions").document()
        
        firestore.runTransaction { transaction ->
            // 1. Read User (Source of Truth)
            val userSnapshot = transaction.get(userRef)
            val currentEarnings = userSnapshot.getDouble("earnings") ?: 0.0
            
            // 2. Validate Balance
            if (currentEarnings < amount) {
                throw com.google.firebase.firestore.FirebaseFirestoreException(
                    "Insufficient balance. Available: $currentEarnings, Requested: $amount",
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                )
            }
            
            // 3. Deduct Balance
            val newEarnings = currentEarnings - amount
            transaction.update(userRef, "earnings", newEarnings)
            
            // 4. Create Withdrawal Record
            val withdrawal = hashMapOf(
                "userId" to userId,
                "amount" to amount,
                "method" to method,
                "details" to details,
                "status" to "pending",
                "timestamp" to FieldValue.serverTimestamp()
            )
            transaction.set(withdrawalsRef, withdrawal)
            
            // 5. Create Transaction Record
            val tx = Transaction(
                id = transactionsRef.id,
                userId = userId,
                amount = -amount,
                description = "Withdrawal Request ($method)",
                status = "pending",
                timestamp = java.util.Date(),
                relatedUserName = userSnapshot.getString("displayName") ?: "",
                relatedUserAvatar = userSnapshot.getString("avatarUrl") ?: ""
            )
            transaction.set(transactionsRef, tx)
        }.await()
    }

    override suspend fun saveTransaction(transaction: com.rkdevstudios.voxly.data.model.Transaction) {
        firestore.collection("transactions").document(transaction.id).set(transaction).await()
    }

    override suspend fun getTransactionsPaged(userId: String, lastDoc: com.google.firebase.firestore.DocumentSnapshot?, limit: Long): Pair<List<com.rkdevstudios.voxly.data.model.Transaction>, com.google.firebase.firestore.DocumentSnapshot?> {
        var query = firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)

        if (lastDoc != null) {
            query = query.startAfter(lastDoc)
        }

        try {
            val snapshot = query.get().await()
            val transactions = snapshot.toObjects(com.rkdevstudios.voxly.data.model.Transaction::class.java)
            val newLastDoc = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null
            return Pair(transactions, newLastDoc)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(emptyList(), null)
        }
    }

    override fun getTransactions(userId: String): kotlinx.coroutines.flow.Flow<List<com.rkdevstudios.voxly.data.model.Transaction>> = callbackFlow {
        val listener = firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.toObjects(com.rkdevstudios.voxly.data.model.Transaction::class.java) ?: emptyList()
                trySend(transactions)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createPaymentOrder(amount: Int): String {
        val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
        
        val data = hashMapOf(
            "amount" to amount
        )

        val result = functions
            .getHttpsCallable("createRazorpayOrder")
            .call(data)
            .await()

        val resultMap = result.data as? Map<String, Any> ?: throw Exception("Invalid response from server")
        return resultMap["orderId"] as? String ?: throw Exception("No Order ID returned")
    }
}
