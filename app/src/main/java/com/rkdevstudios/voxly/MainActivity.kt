package com.rkdevstudios.voxly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.lifecycleScope
import com.rkdevstudios.voxly.navigation.VoxlyNavGraph
import com.rkdevstudios.voxly.ui.theme.GunmetalBlack
import com.rkdevstudios.voxly.ui.theme.RichPurpleDark
import com.rkdevstudios.voxly.ui.theme.VoxlyTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var walletRepository: com.rkdevstudios.voxly.data.repository.WalletRepository
    
    @javax.inject.Inject
    lateinit var userRepository: com.rkdevstudios.voxly.data.repository.UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(com.rkdevstudios.voxly.R.style.Theme_Voxly)
        super.onCreate(savedInstanceState)
        com.rkdevstudios.voxly.util.CoinConstants.loadConfig(com.google.firebase.firestore.FirebaseFirestore.getInstance())
        handleIntent(intent)
        setContent {
            VoxlyTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    RichPurpleDark, // Top
                                    GunmetalBlack   // Bottom
                                )
                            )
                        )
                ) {
                     VoxlyNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val userId = userRepository.getCurrentUserId()
        if (userId != null) {
            lifecycleScope.launch {
                try {
                    android.util.Log.d("Presence", "Writing ONLINE")
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("users").document(userId).update(
                        mapOf(
                            "isOnline" to true,
                            "online" to true,
                            "presence" to "ONLINE",
                            "busy" to false,
                            "lastSeen" to System.currentTimeMillis()
                        )
                    ).addOnSuccessListener {
                        android.util.Log.d("Presence", "Write success")
                    }.addOnFailureListener { exception ->
                        android.util.Log.e("Presence", "Write failed", exception)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Presence", "Write exception", e)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val user = userRepository.currentUserFlow.value
        // If the user is a speaker and has manually toggled themselves online,
        // they run a background foreground service and must remain ONLINE when minimizing.
        if (user != null && user.isSpeaker && user.isOnline) {
            android.util.Log.d("Presence", "Speaker is online with background service, keeping ONLINE on Stop")
            return
        }

        val userId = userRepository.getCurrentUserId()
        if (userId != null) {
            lifecycleScope.launch {
                try {
                    android.util.Log.d("Presence", "Writing OFFLINE")
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("users").document(userId).update(
                        mapOf(
                            "isOnline" to false,
                            "online" to false,
                            "presence" to "OFFLINE"
                        )
                    ).addOnSuccessListener {
                        android.util.Log.d("Presence", "Write success")
                    }.addOnFailureListener { exception ->
                        android.util.Log.e("Presence", "Write failed", exception)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Presence", "Write exception", e)
                }
            }
        }
    }

    private fun handleIntent(intent: android.content.Intent) {
        // No-op for now as ActiveCallActivity handles call acceptance directly.
        // We keep this method stub in case we need to handle other notification intents later.
    }

}
