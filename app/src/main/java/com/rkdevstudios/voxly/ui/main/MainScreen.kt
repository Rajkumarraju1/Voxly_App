package com.rkdevstudios.voxly.ui.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rkdevstudios.voxly.ui.history.HistoryScreen
import com.rkdevstudios.voxly.ui.home.HomeScreen
import com.rkdevstudios.voxly.ui.profile.UserProfileScreen
import com.rkdevstudios.voxly.ui.viewmodel.HomeViewModel

@Composable
fun MainScreen(
    onEditAvatar: (String) -> Unit,
    onEditLanguage: (String) -> Unit,
    onEditTags: (String) -> Unit,
    onEditPayment: () -> Unit,
    onEarningsClick: () -> Unit,
    onWalletClick: () -> Unit,
    onTransactionsClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    onSwitchToListener: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    
    // Redirect back button to Home tab (index 0) if on other tabs; exit app if already on Home.
    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    val items = listOf("Home", "History", "Profile")
    val icons = listOf(Icons.Default.Home, Icons.Default.History, Icons.Default.Person)

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.showWalletEvent.collect { message: String ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            onWalletClick()
        }
    }
    
    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { message: String ->
             Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        // Overlapping container to let content scroll behind the floating bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()) // Only pad top to respect status bar
        ) {
            // 1. Scrollable Screen Content (takes full screen, scrolls under bottom bar)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onWalletClick = onWalletClick,
                        onEarningsClick = { selectedTab = 1 }
                    )
                    1 -> HistoryScreen(
                        homeViewModel = viewModel,
                        onWalletClick = onWalletClick
                    )
                    2 -> UserProfileScreen(
                        viewModel = viewModel,
                        onEditAvatar = onEditAvatar,
                        onWalletClick = onWalletClick,
                        onTransactionsClick = onTransactionsClick,
                        onEditLanguage = onEditLanguage,
                        onEditTags = onEditTags,
                        onEditPayment = onEditPayment,
                        onEarningsClick = onEarningsClick,
                        onAccountSettingsClick = onAccountSettingsClick,
                        onSwitchToListener = onSwitchToListener,
                        onLogout = onLogout
                    )
                }
            }

            // 2. Floating Pill Navigation Bar (placed as overlay at the bottom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141419))
                        .border(1.dp, Color(0xFF2A2A35), CircleShape)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = selectedTab == index
                        
                        val itemModifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFDE3B75).copy(alpha = 0.2f),
                                                    Color(0xFF8F00FF).copy(alpha = 0.2f)
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFDE3B75).copy(alpha = 0.4f),
                                                    Color(0xFF8F00FF).copy(alpha = 0.4f)
                                                )
                                            ),
                                            CircleShape
                                        )
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { selectedTab = index }
                            )

                        Box(
                            modifier = itemModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icons[index],
                                    contentDescription = item,
                                    tint = if (isSelected) Color(0xFFDE3B75) else Color(0xFF8E8E93),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFFDE3B75) else Color(0xFF8E8E93),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
