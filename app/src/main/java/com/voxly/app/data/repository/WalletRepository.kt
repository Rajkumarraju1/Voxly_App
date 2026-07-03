package com.voxly.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.UUID
import java.util.Date

interface WalletRepository {
    suspend fun requestWithdrawal(userId: String, amount: Double, method: String, details: String)
    suspend fun saveTransaction(transaction: com.voxly.app.data.model.Transaction)
    suspend fun getTransactionsPaged(userId: String, lastDoc: com.google.firebase.firestore.DocumentSnapshot?, limit: Long = 20): Pair<List<com.voxly.app.data.model.Transaction>, com.google.firebase.firestore.DocumentSnapshot?>
    suspend fun createPaymentOrder(amount: Int): String
    // Deprecated: Moving to Paged
    fun getTransactions(userId: String): kotlinx.coroutines.flow.Flow<List<com.voxly.app.data.model.Transaction>>
}


