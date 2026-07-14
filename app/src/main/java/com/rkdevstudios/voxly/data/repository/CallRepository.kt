package com.rkdevstudios.voxly.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rkdevstudios.voxly.data.model.Call
import com.rkdevstudios.voxly.data.model.User
import com.rkdevstudios.voxly.data.model.toSupportedCallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

enum class SnapshotSource {
    CACHE,
    SERVER
}

sealed class CallSnapshotResult {
    data class Exists(val call: Call) : CallSnapshotResult()
    
    data class Missing(
        val source: SnapshotSource,
        val hasPendingWrites: Boolean
    ) : CallSnapshotResult()
    
    data class Error(val throwable: Throwable) : CallSnapshotResult()
}

fun CallSnapshotResult.toCallOrNull(): Call? = when (this) {
    is CallSnapshotResult.Exists -> this.call
    else -> null
}

interface CallRepository {
    suspend fun createCall(call: Call): String
    fun listenForIncomingCalls(speakerId: String): Flow<Call?>
    
    @Deprecated("Use observeCall instead", ReplaceWith("observeCall(callId)"))
    fun listenToCall(callId: String): Flow<Call?>
    
    fun observeCall(callId: String): Flow<CallSnapshotResult>
    suspend fun updateCallStatus(callId: String, status: String)
    suspend fun updateCallDuration(callId: String, durationSeconds: Int)
    suspend fun getCallHistory(userId: String): List<Call>
    suspend fun sendVideoRequest(callId: String, requesterId: String)
    suspend fun respondToVideoRequest(callId: String, accept: Boolean)
    suspend fun endCallSession(
        callId: String,
        durationSeconds: Int,
        coinsDeducted: Int
    )
}

class CallRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CallRepository {

    override suspend fun createCall(call: Call): String = withContext(Dispatchers.IO) {
        android.util.Log.d("CallRepository", "Received call.id = ${call.id}")
        val callId = call.id
        val channelId = "channel_$callId"
        
        // Optimize: Add participants array for single-query logic
        val participants = listOf(call.callerId, call.speakerId)
        
        // optimize: Add expireAt (30 days from now)
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 30)
        val expireAt = calendar.time

        val newCall = call.copy(
            id = callId, 
            channelId = channelId,
            participants = participants,
            expireAt = expireAt,
            version = 1
        )
        
        val speakerRef = firestore.collection("users").document(call.speakerId)
        val callerRef = firestore.collection("users").document(call.callerId)
        val callRef = firestore.collection("calls").document(callId)
        android.util.Log.d("CallRepository", "Firestore document ID = $callId")

        firestore.runTransaction { transaction ->
            val speakerSnapshot = transaction.get(speakerRef)
            val callerSnapshot = transaction.get(callerRef)

            // Check if speaker is online
            val isOnline = speakerSnapshot.getBoolean("online") ?: false
            val lastSeen = speakerSnapshot.getLong("lastSeen") ?: 0L
            val now = System.currentTimeMillis()
            if (!isOnline || (now - lastSeen) > 120000L) {
                throw Exception("Speaker is offline")
            }
            
            // Check if speaker has an active call
            val activeCallMap = speakerSnapshot.get("activeCall") as? Map<*, *>
            val hasActiveCall = activeCallMap != null && (activeCallMap["callId"] as? String)?.isNotEmpty() == true
            if (hasActiveCall) {
                throw Exception("Speaker is currently busy")
            }

            // Check if caller already has an active call
            val callerActiveCallMap = callerSnapshot.get("activeCall") as? Map<*, *>
            val callerHasActiveCall = callerActiveCallMap != null && (callerActiveCallMap["callId"] as? String)?.isNotEmpty() == true
            if (callerHasActiveCall) {
                throw Exception("Caller already has an active call")
            }

            // Validate call preferences
            val speakerUser = speakerSnapshot.toObject(User::class.java)
            if (speakerUser != null) {
                val callTypeEnum = try {
                    com.rkdevstudios.voxly.data.model.CallType.valueOf(call.type.uppercase())
                } catch (e: Exception) {
                    com.rkdevstudios.voxly.data.model.CallType.AUDIO
                }
                val mappedType = callTypeEnum.toSupportedCallType()
                if (!speakerUser.callPreferences.getTypedSupportedCallTypes().contains(mappedType)) {
                    throw Exception("Speaker does not accept incoming ${call.type} calls")
                }
            }
            
            // Update activeCall on both speaker and caller
            val activeCallRefMap = mapOf(
                "callId" to callId,
                "role" to "SPEAKER",
                "updatedAt" to now
            )
            val callerCallRefMap = mapOf(
                "callId" to callId,
                "role" to "CALLER",
                "updatedAt" to now
            )
            transaction.update(speakerRef, "activeCall", activeCallRefMap)
            transaction.update(callerRef, "activeCall", callerCallRefMap)
            transaction.update(speakerRef, "busy", true)

            // Create the call
            transaction.set(callRef, newCall)
        }.await()
        
        callId
    }

    override fun listenForIncomingCalls(speakerId: String): Flow<Call?> = callbackFlow {
        val listener = firestore.collection("calls")
            .whereEqualTo("speakerId", speakerId)
            .whereEqualTo("status", "ringing")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val call = snapshot.documents[0].toObject(Call::class.java)
                    trySend(call)
                } else {
                    trySend(null)
                }
            }
        
        awaitClose { listener.remove() }
    }

    @Deprecated("Use observeCall instead", ReplaceWith("observeCall(callId)"))
    override fun listenToCall(callId: String): Flow<Call?> {
        val baseFlow = observeCall(callId)
        return kotlinx.coroutines.flow.flow {
            baseFlow.collect { result ->
                emit(result.toCallOrNull())
            }
        }
    }

    override fun observeCall(callId: String): Flow<CallSnapshotResult> = kotlinx.coroutines.flow.callbackFlow {
        android.util.Log.d("CallRepository", "observeCall: Starting listener for $callId")
        val listener = firestore.collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CallRepository", "observeCall: Error", error)
                    trySend(CallSnapshotResult.Error(error))
                    return@addSnapshotListener
                }

                val source = if (snapshot?.metadata?.isFromCache == true) SnapshotSource.CACHE else SnapshotSource.SERVER
                val hasPendingWrites = snapshot?.metadata?.hasPendingWrites() ?: false

                if (snapshot != null && snapshot.exists()) {
                    val call = snapshot.toObject(Call::class.java)
                    if (call != null) {
                        android.util.Log.d("CallRepository", "observeCall: Received update for $callId. Status=${call.status}, source=$source")
                        trySend(CallSnapshotResult.Exists(call))
                    } else {
                        android.util.Log.d("CallRepository", "observeCall: Document exists but object is null. source=$source")
                        trySend(CallSnapshotResult.Missing(source, hasPendingWrites))
                    }
                } else {
                    android.util.Log.d("CallRepository", "observeCall: Document does not exist. source=$source")
                    trySend(CallSnapshotResult.Missing(source, hasPendingWrites))
                }
            }
        awaitClose {
            android.util.Log.d("CallRepository", "observeCall: Closing listener for $callId")
            listener.remove()
        }
    }

    override suspend fun updateCallStatus(callId: String, status: String) = withContext(Dispatchers.IO) {
        val callRef = firestore.collection("calls").document(callId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(callRef)
            val call = snapshot.toObject(Call::class.java)
            
            if (call != null) {
                val speakerRef = firestore.collection("users").document(call.speakerId)
                val callerRef = firestore.collection("users").document(call.callerId)
                
                // Read ALL snapshots first to respect Firestore transaction constraints (reads before writes)
                val speakerSnap = transaction.get(speakerRef)
                val callerSnap = transaction.get(callerRef)

                // Now execute all updates/writes
                transaction.update(callRef, "status", status)
                
                // If call is ending/declined/cancelled/timeout, free the participants
                if (status == "ended" || status == "declined" || status == "cancelled" || status == "timeout") {
                    val speakerCallId = (speakerSnap.get("activeCall") as? Map<*, *>)?.get("callId") as? String
                    if (speakerCallId == callId) {
                        transaction.update(speakerRef, "activeCall", null)
                        transaction.update(speakerRef, "busy", false)
                    }
                    
                    val callerCallId = (callerSnap.get("activeCall") as? Map<*, *>)?.get("callId") as? String
                    if (callerCallId == callId) {
                        transaction.update(callerRef, "activeCall", null)
                    }
                } else {
                    val speakerCallId = (speakerSnap.get("activeCall") as? Map<*, *>)?.get("callId") as? String
                    if (speakerCallId == callId) {
                        transaction.update(speakerRef, "activeCall.status", status)
                    }
                    
                    val callerCallId = (callerSnap.get("activeCall") as? Map<*, *>)?.get("callId") as? String
                    if (callerCallId == callId) {
                        transaction.update(callerRef, "activeCall.status", status)
                    }
                }
            }
        }.await()
        Unit
    }

    override suspend fun updateCallDuration(callId: String, durationSeconds: Int) = withContext(Dispatchers.IO) {
        firestore.collection("calls").document(callId)
            .update("duration", durationSeconds)
            .await()
        Unit
    }

    // New: Pagination Support
    suspend fun getCallHistoryPaged(userId: String, lastDoc: com.google.firebase.firestore.DocumentSnapshot?, limit: Long = 20): Pair<List<Call>, com.google.firebase.firestore.DocumentSnapshot?> = withContext(Dispatchers.IO) {
        try {
            var query = firestore.collection("calls")
                .whereArrayContains("participants", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)

            if (lastDoc != null) {
                query = query.startAfter(lastDoc)
            }

            val snapshot = query.get().await()
            val calls = snapshot.toObjects(Call::class.java)
            val newLastDoc = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null
            
            Pair(calls, newLastDoc)
        } catch (e: Exception) {
            // Fallback for old data without 'participants' array (Backward Compatibility)
            // Or if Index is missing.
            // For now, we return empty or try the old method if this fails
            e.printStackTrace()
            // Fallback to non-paged old method if query fails (e.g. index missing)
            Pair(getCallHistory(userId), null) 
        }
    }

    // Deprecated: Kept for fallback, but logic updated to sort correctly
    override suspend fun getCallHistory(userId: String): List<Call> = withContext(Dispatchers.IO) {
        // ... (Old logic ideally replaced by Paged)
        // For simplicity, we just return the old "Merge" logic if paged fails
        val asCaller = firestore.collection("calls")
             .whereEqualTo("callerId", userId)
             .get().await().toObjects(Call::class.java)
        val asSpeaker = firestore.collection("calls")
             .whereEqualTo("speakerId", userId)
             .get().await().toObjects(Call::class.java)
         
        // manual distinct to avoid duplicates if migration happens half-way
        val combined = (asCaller + asSpeaker).distinctBy { it.id }
        combined.sortedByDescending { it.createdAt }
    }

    suspend fun clearHistory(userId: String) = withContext(Dispatchers.IO) {
        // 1. Try deleting via 'participants' (Efficient, New Data)
        val calls = firestore.collection("calls")
            .whereArrayContains("participants", userId)
            .limit(500) 
            .get().await()

        val batch = firestore.batch()
        var deleteCount = 0
        for (doc in calls.documents) {
            batch.delete(doc.reference)
            deleteCount++
        }
        
        // 2. Fallback: Try deleting via 'callerId' (Old Data)
        if (deleteCount < 500) {
             val asCaller = firestore.collection("calls")
                 .whereEqualTo("callerId", userId)
                 .limit((500 - deleteCount).toLong())
                 .get().await()
             for (doc in asCaller.documents) {
                 batch.delete(doc.reference)
                 deleteCount++
             }
        }
        
        // 3. Fallback: Try deleting via 'speakerId' (Old Data)
        if (deleteCount < 500) {
             val asSpeaker = firestore.collection("calls")
                 .whereEqualTo("speakerId", userId)
                 .limit((500 - deleteCount).toLong())
                 .get().await()
             for (doc in asSpeaker.documents) {
                 batch.delete(doc.reference)
                 deleteCount++
             }
        }

        if (deleteCount > 0) {
            batch.commit().await()
        }
    }
    
    // New: Allow deleting specific selected calls
    suspend fun deleteCalls(callIds: List<String>) = withContext(Dispatchers.IO) {
        if (callIds.isEmpty()) return@withContext
        
        // Firestore batch limit is 500. We will chunk it.
        callIds.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { id ->
                val ref = firestore.collection("calls").document(id)
                batch.delete(ref)
            }
            batch.commit().await()
        }
    }
    
    suspend fun pruneOldHistory(userId: String) = withContext(Dispatchers.IO) {
        // Client-side pruning fallback (if TTL not set)
        val thirtyDaysAgo = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -30)
        }.time
        
        val oldCalls = firestore.collection("calls")
            .whereArrayContains("participants", userId)
            .whereLessThan("createdAt", thirtyDaysAgo)
            .limit(100)
            .get().await()
            
        if (!oldCalls.isEmpty) {
             val batch = firestore.batch()
             for (doc in oldCalls.documents) {
                 batch.delete(doc.reference)
             }
             batch.commit().await()
        }
    }

    override suspend fun sendVideoRequest(callId: String, requesterId: String) = withContext(Dispatchers.IO) {
        firestore.collection("calls").document(callId)
            .update(
                mapOf(
                    "videoRequestStatus" to "pending",
                    "videoRequestBy" to requesterId
                )
            ).await()
        Unit
    }

    override suspend fun respondToVideoRequest(callId: String, accept: Boolean) = withContext(Dispatchers.IO) {
        val updates = if (accept) {
            mapOf(
                "videoRequestStatus" to "accepted",
                "type" to "video"
            )
        } else {
            mapOf(
                "videoRequestStatus" to "declined"
            )
        }
        firestore.collection("calls").document(callId).update(updates).await()
        Unit
    }

    override suspend fun endCallSession(
        callId: String,
        durationSeconds: Int,
        coinsDeducted: Int
    ) = withContext(Dispatchers.IO) {
        val callRef = firestore.collection("calls").document(callId)
        
        firestore.runTransaction { transaction ->
            val callSnapshot = transaction.get(callRef)
            val call = callSnapshot.toObject(Call::class.java) ?: throw Exception("Call not found")
            
            // Sole idempotency guard
            if (call.settled) {
                android.util.Log.d("BILLING_AUDIT", "endCallSession: Call already settled. Skipping transaction. callId=$callId")
                return@runTransaction
            }

            val callerRef = firestore.collection("users").document(call.callerId)
            val speakerRef = firestore.collection("users").document(call.speakerId)
            
            val callerSnapshot = transaction.get(callerRef)
            val speakerSnapshot = transaction.get(speakerRef)
            
            // --- SECURITY / CONSISTENCY CHECK ---
            // Calculate values server-side (Repo-side) using dynamic configuration
            val config = com.rkdevstudios.voxly.util.CoinConstants.currentConfig
            val speakerEarned = coinsDeducted * config.speakerRate
            val platformEarned = coinsDeducted * config.platformRate
            val grossRevenue = speakerEarned + platformEarned
            // ------------------------------------

            // 1. Deduct coins from Caller
            val currentCoins = callerSnapshot.getLong("coins")?.toInt() ?: 0
            val newCoins = (currentCoins - coinsDeducted).coerceAtLeast(0)
            android.util.Log.d("BILLING_AUDIT", "endCallSession Transaction executing. callId=$callId | callerId=${call.callerId} | speakerId=${call.speakerId} | durationSeconds=$durationSeconds | coinsDeducted=$coinsDeducted | currentCoins=$currentCoins | newCoins=$newCoins")
            transaction.update(callerRef, "coins", newCoins)
            
            // 2. Add earnings to Speaker
            val currentEarnings = speakerSnapshot.getDouble("earnings") ?: 0.0
            transaction.update(speakerRef, "earnings", currentEarnings + speakerEarned)
            
            // 3. Free the speaker and caller activeCall references with ownership validation
            val speakerCallId = (speakerSnapshot.get("activeCall") as? Map<*, *>)?.get("callId") as? String
            if (speakerCallId == callId) {
                transaction.update(speakerRef, "activeCall", null)
                transaction.update(speakerRef, "busy", false)
            }
            val callerCallId = (callerSnapshot.get("activeCall") as? Map<*, *>)?.get("callId") as? String
            if (callerCallId == callId) {
                transaction.update(callerRef, "activeCall", null)
            }
            
            // 4. Update Call Document with auditing parameters and settled info
            val rateForType = if (call.type.lowercase() == "video") config.videoCoinsPerMinute.toDouble() else config.audioCoinsPerMinute.toDouble()
            transaction.update(callRef, mapOf(
                "status" to "ended",
                "duration" to durationSeconds,
                "coinsDeducted" to coinsDeducted,
                "grossRevenue" to grossRevenue,
                "speakerEarned" to speakerEarned,
                "platformEarned" to platformEarned,
                "pricingVersion" to config.pricingVersion,
                "audioCoinsPerMinute" to config.audioCoinsPerMinute,
                "videoCoinsPerMinute" to config.videoCoinsPerMinute,
                "speakerRate" to config.speakerRate,
                "platformRate" to config.platformRate,
                "settled" to true,
                "settledAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "settledByUid" to call.callerId,
                "settlementVersion" to 1,
                "settledCallerCoinsSpent" to coinsDeducted,
                "settledSpeakerPayout" to speakerEarned,
                "settledDurationSeconds" to durationSeconds,
                "settledCallType" to call.type,
                "settledRatePerMinute" to rateForType,
                "settledSpeakerRate" to config.speakerRate
            ))

            // 5. Create Transactions
            val timestamp = java.util.Date()
            
            // Speaker Transaction (Credit) -> Related to Caller
            if (speakerEarned > 0) {
                val speakerTxRef = firestore.collection("transactions").document()
                val speakerTx = com.rkdevstudios.voxly.data.model.Transaction(
                    id = speakerTxRef.id,
                    userId = call.speakerId,
                    amount = speakerEarned,
                    coins = 0, // Speaker gets earnings in currency
                    description = "Call Earnings",
                    status = "success",
                    timestamp = timestamp,
                    // Denormalization
                    relatedUserId = call.callerId,
                    relatedUserName = call.callerName,
                    relatedUserAvatar = call.callerAvatar
                )
                transaction.set(speakerTxRef, speakerTx)
            }

            // Caller Transaction (Debit) -> Related to Speaker
            if (coinsDeducted > 0) {
                val callerTxRef = firestore.collection("transactions").document()
                val callerTx = com.rkdevstudios.voxly.data.model.Transaction(
                    id = callerTxRef.id,
                    userId = call.callerId,
                    amount = -grossRevenue, // Negative amount for spending
                    coins = coinsDeducted,
                    description = "Call Service",
                    status = "success",
                    timestamp = timestamp,
                    // Denormalization
                    relatedUserId = call.speakerId,
                    relatedUserName = call.speakerName,
                    relatedUserAvatar = call.speakerAvatar
                )
                transaction.set(callerTxRef, callerTx)
            }
        }.await()
        Unit
    }
}
