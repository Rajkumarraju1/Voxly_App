package com.voxly.app

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
import com.voxly.app.navigation.VoxlyNavGraph
import com.voxly.app.ui.theme.GunmetalBlack
import com.voxly.app.ui.theme.RichPurpleDark
import com.voxly.app.ui.theme.VoxlyTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var walletRepository: com.voxly.app.data.repository.WalletRepository
    
    @javax.inject.Inject
    lateinit var userRepository: com.voxly.app.data.repository.UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun handleIntent(intent: android.content.Intent) {
        // No-op for now as ActiveCallActivity handles call acceptance directly.
        // We keep this method stub in case we need to handle other notification intents later.
    }

}
