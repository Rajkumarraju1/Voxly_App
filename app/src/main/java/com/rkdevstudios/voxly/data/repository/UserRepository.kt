package com.rkdevstudios.voxly.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rkdevstudios.voxly.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collect
import com.rkdevstudios.voxly.data.local.LocalUserDataSource
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

interface UserRepository {
    val currentUserFlow: StateFlow<User?>
    suspend fun saveUser(user: User)
    suspend fun getUser(userId: String): User?
    fun getUserFlow(userId: String): Flow<User?>
    fun getCurrentUserId(): String?
    suspend fun updateUser(user: User)
    suspend fun getSpeakers(currentUserLanguages: List<String>, lastVisible: com.google.firebase.firestore.DocumentSnapshot? = null, source: com.google.firebase.firestore.Source = com.google.firebase.firestore.Source.DEFAULT): Pair<List<User>, com.google.firebase.firestore.DocumentSnapshot?>
    fun listenToUser(userId: String)
    suspend fun rateSpeaker(speakerId: String, rating: Int)
    suspend fun updateFcmToken(token: String)
    suspend fun blockUser(userId: String)
    suspend fun reportUser(reportedUserId: String, reason: String, description: String)
    suspend fun updateUserCoins(userId: String, newBalance: Double)
    fun logout()
    suspend fun deleteAccount()
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val localUserDataSource: LocalUserDataSource // Injected
) : UserRepository {

    private val _currentUserFlow = kotlinx.coroutines.flow.MutableStateFlow<User?>(null)
    override val currentUserFlow: kotlinx.coroutines.flow.StateFlow<User?> =
        _currentUserFlow.asStateFlow()

    override suspend fun saveUser(user: User) {
        val userId = auth.currentUser?.uid ?: return
        val finalUser = user.copy(id = userId)
        firestore.collection("users").document(userId).set(finalUser).await()
        _currentUserFlow.emit(finalUser)
    }

    override suspend fun getUser(userId: String): User? {
        val snapshot = firestore.collection("users").document(userId).get().await()
        val user = snapshot.toObject(User::class.java) ?: return null
        val normalized = normalizePresence(user, System.currentTimeMillis())
        if (userId == getCurrentUserId()) {
            _currentUserFlow.emit(normalized)
        }
        return normalized
    }

    override fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        trySend(normalizePresence(user, System.currentTimeMillis()))
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun updateUser(user: User) {
        val userId = user.id.takeIf { it.isNotEmpty() } ?: auth.currentUser?.uid ?: return
        if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
            android.util.Log.d("ONLINE_TRACE", "WRITER=UserRepository.updateUser online=${user.isOnline} lastSeen=${user.lastSeen}")
        }
        try {
            firestore.collection("users").document(userId).set(user)
                .addOnSuccessListener {
                    if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
                        android.util.Log.d("ONLINE_TRACE", "WRITER=UserRepository.updateUser SUCCESS online=${user.isOnline}")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ONLINE_TRACE", "WRITER=UserRepository.updateUser FAILED", e)
                }
                .await()
            _currentUserFlow.emit(user)
        } catch (e: Exception) {
            android.util.Log.e("ONLINE_TRACE", "WRITER=UserRepository.updateUser EXCEPTION", e)
            throw e
        }
    }

    override suspend fun getSpeakers(
        currentUserLanguages: List<String>,
        lastVisible: com.google.firebase.firestore.DocumentSnapshot?,
        source: com.google.firebase.firestore.Source
    ): Pair<List<User>, com.google.firebase.firestore.DocumentSnapshot?> {
        var query = firestore.collection("users")
            .whereEqualTo("speaker", true)
            // Show recently active users first to ensure we don't load dead accounts at the top
            .orderBy("lastSeen", com.google.firebase.firestore.Query.Direction.DESCENDING)

        if (currentUserLanguages.isNotEmpty()) {
            query = query.whereArrayContainsAny("languages", currentUserLanguages.take(10))
        }

        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        // Aggressive Caching Strategy:
        // 1. Try Cache First
        var snapshot: com.google.firebase.firestore.QuerySnapshot? = null
        try {
            if (source == com.google.firebase.firestore.Source.DEFAULT) {
               snapshot = query.limit(20).get(com.google.firebase.firestore.Source.CACHE).await()
            }
        } catch (e: Exception) {
            // Cache miss or error, proceed to server
        }

        // 2. If Cache is empty or failed, go to Server
        if (snapshot == null || snapshot?.isEmpty == true) {
             try {
                snapshot = query.limit(20).get(com.google.firebase.firestore.Source.SERVER).await()
             } catch (e: Exception) {
                 e.printStackTrace()
                 return Pair(emptyList(), null)
             }
        }
        
        val finalSnapshot = snapshot ?: return Pair(emptyList(), null)
        var users = finalSnapshot.toObjects(User::class.java)

        // Monitor staleness: If online in DB but lastSeen is old, mark as offline locally
        val currentTime = System.currentTimeMillis()
        users = users.map { normalizePresence(it, currentTime) }

        val newLastVisible =
            if (finalSnapshot.documents.isNotEmpty()) finalSnapshot.documents.last() else null

        return Pair(users, newLastVisible)
    }

    private fun normalizePresence(user: User, currentTime: Long): User {
        if (!user.isOnline) {
            // Manual Offline always wins
            return user
        }
        val isRecent = user.lastSeen > 0L && (currentTime - user.lastSeen) < 120000L
        return if (isRecent) {
            user
        } else {
            user.copy(isOnline = false)
        }
    }

    override fun listenToUser(userId: String) {
        // 1. Fast load from local cache
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            localUserDataSource.userData.collect { cachedUser ->
                if (cachedUser != null && _currentUserFlow.value == null) {
                    val normalized = normalizePresence(cachedUser, System.currentTimeMillis())
                    _currentUserFlow.emit(normalized)
                }
            }
        }

        // 2. Real-time listener
        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
                    android.util.Log.d("ONLINE_TRACE", "READER=UserRepository.addSnapshotListener fired: error=$e, exists=${snapshot?.exists()}")
                }
                if (e != null) {
                    android.util.Log.e("ONLINE_TRACE", "READER=UserRepository.addSnapshotListener error", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
                        android.util.Log.d("ONLINE_TRACE", "READER=UserRepository.addSnapshotListener rawData=${snapshot.data}")
                    }
                    val rawOnline = snapshot.getBoolean("online")
                    val rawIsOnline = snapshot.getBoolean("isOnline")
                    val rawLastSeen = snapshot.getLong("lastSeen")
                    if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
                        android.util.Log.d("ONLINE_TRACE", "READER=UserRepository.addSnapshotListener rawDocFields: online=$rawOnline, isOnline=$rawIsOnline, lastSeen=$rawLastSeen")
                    }
                    
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
                            android.util.Log.d("ONLINE_TRACE", "READER=UserRepository.addSnapshotListener Deserialized User: isOnline=${user.isOnline}, lastSeen=${user.lastSeen}")
                        }
                        val normalized = normalizePresence(user, System.currentTimeMillis())
                        if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
                            android.util.Log.d("ONLINE_TRACE", "READER=UserRepository.addSnapshotListener Normalized User: isOnline=${normalized.isOnline}, lastSeen=${normalized.lastSeen}")
                        }
                        _currentUserFlow.value = normalized
                        // Update cache
                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            localUserDataSource.saveUser(normalized)
                        }
                    }
                }
            }
    }

    override suspend fun rateSpeaker(speakerId: String, rating: Int) {
        val speakerRef = firestore.collection("users").document(speakerId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(speakerRef)
            val currentSum = snapshot.getDouble("ratingSum") ?: 0.0
            val currentCount = snapshot.getLong("ratingCount") ?: 0

            transaction.update(speakerRef, "ratingSum", currentSum + rating)
            transaction.update(speakerRef, "ratingCount", currentCount + 1)
        }.await()
    }

    override suspend fun blockUser(userId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(currentUserId)
            .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
            .await()
    }

    override suspend fun reportUser(reportedUserId: String, reason: String, description: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val report = hashMapOf(
            "reporterId" to currentUserId,
            "reportedUserId" to reportedUserId,
            "reason" to reason,
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pending"
        )
        firestore.collection("reports").add(report).await()
    }

    override suspend fun updateFcmToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateUserCoins(userId: String, newBalance: Double) {
        firestore.collection("users").document(userId)
            .update("coins", newBalance)
            .await()

        // Update local flow if it matches current user
        val currentUser = _currentUserFlow.value
        if (currentUser != null && currentUser.id == userId) {
            _currentUserFlow.emit(currentUser.copy(coins = newBalance))
        }
    }

    override fun logout() {
        auth.signOut()
        _currentUserFlow.value = null
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            localUserDataSource.clear()
        }
    }

    override suspend fun deleteAccount() {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Invalidate FCM token locally
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Write deletion request document using transaction to enforce the soft lock policy
        try {
            val docRef = firestore.collection("accountDeletionRequests").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "pending" || status == "processing" || status == "completed") {
                        throw Exception("Account deletion is already requested or completed.")
                    }
                }
                
                val requestData = hashMapOf(
                    "userId" to userId,
                    "requestedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "processedAt" to null,
                    "status" to "pending",
                    "appVersion" to "1.1",
                    "platform" to "android"
                )
                transaction.set(docRef, requestData)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        // 3. Cleanup local session
        logout()
    }
}


