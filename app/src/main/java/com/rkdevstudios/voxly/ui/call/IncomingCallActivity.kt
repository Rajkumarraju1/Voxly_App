package com.rkdevstudios.voxly.ui.call

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.rkdevstudios.voxly.data.model.User
import com.rkdevstudios.voxly.ui.theme.VoxlyTheme
import com.rkdevstudios.voxly.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Wake up screen logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Retrieve call details from Intent
        val callId = intent.getStringExtra("CALL_ID") ?: ""
        val callerId = intent.getStringExtra("CALLER_ID") ?: ""
        val callerName = intent.getStringExtra("CALLER_NAME") ?: "Unknown"
        val callerAvatar = intent.getStringExtra("CALLER_AVATAR") ?: ""
        val rate = intent.getIntExtra("RATE", 10)
        val type = intent.getStringExtra("TYPE") ?: "audio"
        
        homeViewModel.setIncomingCallFromIntent(callId, callerId, callerName, callerAvatar, rate, type)

        setContent {
            VoxlyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val callState by homeViewModel.callState.collectAsState()
                    
                    LaunchedEffect(callState) {
                        if (callState is com.rkdevstudios.voxly.ui.viewmodel.CallState.Idle) {
                            finish()
                        }
                    }

                    IncomingCallScreen(
                        callerName = callerName,
                        callerAvatarUrl = callerAvatar,
                        rate = rate,
                        onAccept = {
                            lifecycleScope.launch {
                                homeViewModel.acceptCall()
                                // Launch ActiveCallActivity explicitly
                                val intent = android.content.Intent(this@IncomingCallActivity, com.rkdevstudios.voxly.ui.call.ActiveCallActivity::class.java).apply {
                                    putExtra("CALL_ID", callId)
                                    putExtra("CALLER_ID", callerId)
                                    putExtra("CALLER_NAME", callerName)
                                    putExtra("CALLER_AVATAR", callerAvatar)
                                    putExtra("RATE", rate)
                                    putExtra("TYPE", type)
                                }
                                startActivity(intent)
                                finish()
                            }
                        },
                        onDecline = {
                            homeViewModel.declineCall()
                            finish()
                        }
                    )
                }
            }
        }
    }
}
