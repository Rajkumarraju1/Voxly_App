package com.rkdevstudios.voxly.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.rkdevstudios.voxly.data.model.CallPreferences
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rkdevstudios.voxly.data.model.User
import com.rkdevstudios.voxly.ui.speaker.SpeakerSettingsScreen
import com.rkdevstudios.voxly.ui.viewmodel.HomeViewModel
import com.rkdevstudios.voxly.util.AvatarHelper

@Composable
fun UserProfileScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onEditAvatar: (String) -> Unit,
    onWalletClick: () -> Unit,
    onTransactionsClick: () -> Unit,
    onEditLanguage: (String) -> Unit,
    onEditTags: (String) -> Unit,
    onEditPayment: () -> Unit,
    onEarningsClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    onSwitchToListener: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    currentUser?.let { user ->
        if (user.isSpeaker) { 
              SpeakerSettingsScreen(
                  user = user, 
                  onToggleOnline = { viewModel.toggleOnlineStatus(it) },
                  onEditProfile = onEditAvatar,
                  onEditLanguage = onEditLanguage,
                  onEditTags = onEditTags,
                  onEditPayment = onEditPayment,
                  onEarningsClick = onEarningsClick,
                  onAccountSettingsClick = onAccountSettingsClick,
                  onUpdatePreferences = { viewModel.updateCallPreferences(it) },
                  onRequestDowngrade = { viewModel.submitDowngradeRequest() },
                  onLogout = onLogout
              )
        } else {
             RegularUserProfile(
                user = user, 
                onSwitchToListener = onSwitchToListener,
                onEditAvatar = onEditAvatar,
                onWalletClick = onWalletClick,
                onTransactionsClick = onTransactionsClick,
                onEditLanguage = onEditLanguage,
                onEditTags = onEditTags,
                onAccountSettingsClick = onAccountSettingsClick,
                onLogout = onLogout
             )
        }
    } ?: run {
         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun RegularUserProfile(
    user: User,
    onSwitchToListener: () -> Unit,
    onEditAvatar: (String) -> Unit,
    onWalletClick: () -> Unit = {},
    onTransactionsClick: () -> Unit = {},
    onEditLanguage: (String) -> Unit,
    onEditTags: (String) -> Unit,
    onAccountSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val avatarResId = remember(user.avatarUrl) {
        AvatarHelper.getDrawableId(context, user.avatarUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Dark background
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Premium Profile Header Card with Pink/Purple Gradient Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x22DE3B75), // Muted Pink Glow
                            Color(0xFF141419)  // Dark Base
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(Color(0x33DE3B75), Color(0x118F00FF))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            // Edit Pencil button on the top-right
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1A24))
                    .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.4f), CircleShape)
                    .clickable { onEditAvatar(user.gender) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color(0xFFDE3B75),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                
                // Avatar with Circular Gradient Border Ring (without camera badge overlay)
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .border(
                            3.dp,
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFDE3B75), Color(0xFF8F00FF))
                            ),
                            CircleShape
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                ) {
                    if (avatarResId != 0) {
                        Image(
                            painter = painterResource(id = avatarResId),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1E24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user.displayName.ifEmpty { "User Name" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // 2. Settings Menu List Container Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141419))
                .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ProfileMenuItem(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    text = "Wallet",
                    isWallet = true,
                    onClick = onWalletClick
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))
                
                ProfileMenuItem(
                    icon = Icons.Rounded.Receipt,
                    text = "Transactions",
                    iconTint = Color(0xFFB060FF),
                    onClick = onTransactionsClick
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))

                val currentLang = user.languages.firstOrNull() ?: "English"
                ProfileMenuItem(
                    icon = Icons.Rounded.Language,
                    text = "Language Settings",
                    subtitle = currentLang,
                    iconTint = Color(0xFF9F80FF),
                    onClick = { onEditLanguage(currentLang) }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Rounded.SwapHoriz,
                    text = "Switch to Listener",
                    iconTint = Color(0xFFFF007F),
                    onClick = onSwitchToListener
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))

                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                ProfileMenuItem(
                    icon = Icons.Rounded.SupportAgent,
                    text = "Help & Support",
                    iconTint = Color(0xFF9C47FF),
                    onClick = { 
                         uriHandler.openUri(context.getString(com.rkdevstudios.voxly.R.string.url_support))
                    }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Rounded.ManageAccounts,
                    text = "Account Settings",
                    iconTint = Color(0xFFDE3B75),
                    onClick = onAccountSettingsClick
                )
            }
        }

        // 3. Separate Log Out Card Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141419))
                .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(24.dp))
        ) {
             ProfileMenuItem(
                 icon = Icons.AutoMirrored.Rounded.ExitToApp,
                 text = "Log Out",
                 isLogout = true,
                 onClick = onLogout
             )
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    subtitle: String? = null,
    iconTint: Color = Color(0xFFB060FF),
    isWallet: Boolean = false,
    isLogout: Boolean = false,
    onClick: () -> Unit = {}
) {
    val containerShape = RoundedCornerShape(16.dp)
    
    val containerModifier = Modifier
        .size(52.dp)
        .clip(containerShape)
        .then(
            when {
                isWallet -> {
                    Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFDE3B75).copy(alpha = 0.25f),
                                    Color(0xFF8F00FF).copy(alpha = 0.25f)
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
                            containerShape
                        )
                }
                isLogout -> {
                    Modifier
                        .background(Color(0xFFE53935).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFE53935).copy(alpha = 0.2f), containerShape)
                }
                else -> {
                    Modifier
                        .background(Color(0xFF1E1D26))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), containerShape)
                }
            }
        )

    val currentIconTint = when {
        isWallet -> Color.White
        isLogout -> Color(0xFFE53935)
        else -> iconTint
    }

    val textColor = if (isLogout) Color(0xFFE53935) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = containerModifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = currentIconTint,
                modifier = Modifier.size(24.dp) // Consistent 24dp size
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = 16.sp
                )
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                )
            }
        }

        // Chevron Arrow on the right
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}
