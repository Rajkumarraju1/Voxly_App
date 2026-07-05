package com.rkdevstudios.voxly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rkdevstudios.voxly.util.AvatarHelper
import com.rkdevstudios.voxly.ui.theme.VoxlyDimens
import com.rkdevstudios.voxly.ui.theme.VoxlySpecs
import com.rkdevstudios.voxly.ui.theme.VoxlyAnim
import com.rkdevstudios.voxly.data.model.SpeakerAvailabilityUi

@Composable
fun getTagIcon(tag: String): androidx.compose.ui.graphics.vector.ImageVector? {
    return when (tag.lowercase().trim()) {
        "cinema", "movies" -> Icons.Default.Movie
        "deep talks", "talks", "chat" -> Icons.Default.ChatBubble
        "travel", "trip" -> Icons.Default.Flight
        "music", "song" -> Icons.Default.MusicNote
        "coffee", "tea" -> Icons.Default.Coffee
        else -> null
    }
}

@Composable
fun ProfileCard(
    name: String, 
    tags: List<String>, 
    avatarUrl: String,
    tagline: String = "",
    rating: Double = 0.0,
    ratingCount: Int = 0,
    availability: SpeakerAvailabilityUi,
    showActions: Boolean = true,
    onCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onReport: ((String, String) -> Unit)? = null,
    onBlock: (() -> Unit)? = null,
    
    // Configurable state parameters to allow reuse in search / recommendations
    showOnlineIndicator: Boolean = true,
    showBusyState: Boolean = true,
    showPricing: Boolean = true,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    callPriceCoins: Int = 10,
    videoPriceCoins: Int = com.rkdevstudios.voxly.util.CoinConstants.VIDEO_COINS_PER_MIN
) {
    var showMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showReportDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showBlockDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var reportReason by remember { androidx.compose.runtime.mutableStateOf("Spam") }
    val context = LocalContext.current
    val avatarResId = remember(avatarUrl) {
        AvatarHelper.getDrawableId(context, avatarUrl)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VoxlyDimens.SpacingBetweenCards / 2)
            .padding(horizontal = VoxlyDimens.CardHorizontalPadding),
        elevation = CardDefaults.cardElevation(defaultElevation = VoxlySpecs.CardElevation),
        border = BorderStroke(VoxlyDimens.BorderWidthThin, Color(0xFF2A2A35)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
        shape = RoundedCornerShape(VoxlyDimens.CardCornerRadius)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VoxlySpecs.ProfileImageAspectRatio)
                .background(Color(0xFF0A0A0A))
        ) {
            if (avatarResId != 0) {
                Image(
                    painter = painterResource(id = avatarResId),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp).align(Alignment.Center)
                )
            }

            // Gradient Scrim overlay from bottom to top (Fixed overlay bounds)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = VoxlySpecs.GradientOverlayStart
                        )
                    )
            )

            // Top right more actions menu overlay
            if (onReport != null || onBlock != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(VoxlyDimens.DefaultContentPadding),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box { // Local Anchor Box to correctly position dropdown under three dots icon
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (onReport != null) {
                                DropdownMenuItem(
                                    text = { Text("Report") },
                                    onClick = { 
                                        showMenu = false 
                                        showReportDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                                )
                            }
                            if (onBlock != null) {
                                DropdownMenuItem(
                                    text = { Text("Block") },
                                    onClick = { 
                                        showMenu = false
                                        showBlockDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom content overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(VoxlyDimens.DefaultContentPadding),
                horizontalAlignment = Alignment.Start
            ) {
                // Name Row - Verification badge completely removed per guidelines
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Tagline (Bio)
                if (tagline.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }

                // Rating Row with Yellow Star
                if (ratingCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star, 
                            contentDescription = null, 
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(VoxlyDimens.SmallIconSize)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f (%d)", rating / ratingCount, ratingCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Tags Row with BrandPrimary border, background, and matching icon
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        tags.forEach { tag ->
                            val emoji = AvatarHelper.getTagEmoji(tag)
                             Surface(
                                shape = RoundedCornerShape(VoxlyDimens.TagCornerRadius),
                                border = BorderStroke(VoxlyDimens.BorderWidthThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$emoji $tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                if (showActions) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SpeakerCallActions(
                        availability = availability,
                        audioRate = callPriceCoins,
                        videoRate = videoPriceCoins,
                        showPricing = showPricing,
                        onCallClick = onCallClick,
                        onVideoCallClick = onVideoCallClick
                    )
                }
            }
        }
    }
    
    // --- DIALOGS ---
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report User") },
            text = {
                Column {
                    listOf("Spam", "Abusive", "Inappropriate", "Other").forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { reportReason = reason }
                        ) {
                            RadioButton(
                                selected = (reportReason == reason),
                                onClick = { reportReason = reason }
                            )
                            Text(text = reason, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReport?.invoke(reportReason, "Reported from Home")
                        showReportDialog = false
                    }
                ) { Text("Report") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block User") },
            text = { Text("Block this user? You won't see them again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBlock?.invoke()
                        showBlockDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }
}
