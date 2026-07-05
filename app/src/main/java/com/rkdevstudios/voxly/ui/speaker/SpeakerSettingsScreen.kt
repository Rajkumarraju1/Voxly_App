package com.rkdevstudios.voxly.ui.speaker

import androidx.compose.material.icons.filled.Close

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkdevstudios.voxly.data.model.User
import com.rkdevstudios.voxly.data.model.deriveAvailability
import com.rkdevstudios.voxly.data.model.SupportedCallType

@Composable
fun SpeakerSettingsScreen(
    user: User,
    onToggleOnline: (Boolean) -> Unit,
    onEditProfile: (String) -> Unit,
    onEditLanguage: (String) -> Unit,
    onEditTags: (String) -> Unit,
    onEditPayment: () -> Unit,
    onEarningsClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    onUpdatePreferences: (com.rkdevstudios.voxly.data.model.CallPreferences) -> Unit,
    onRequestDowngrade: () -> Unit,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDowngradeDialog by remember { mutableStateOf(false) }

    // Motion animation state triggers on profile launch
    var isLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isLaunched = true
    }

    val cardAlpha by animateFloatAsState(
        targetValue = if (isLaunched) 1f else 0f,
        animationSpec = tween(durationMillis = 350)
    )
    val cardOffsetY by animateDpAsState(
        targetValue = if (isLaunched) 0.dp else 8.dp,
        animationSpec = tween(durationMillis = 350)
    )
    val avatarScale by animateFloatAsState(
        targetValue = if (isLaunched) 1f else 0.95f,
        animationSpec = tween(durationMillis = 300)
    )

    // Custom Crown ImageVector for the Speaker Badge
    val crownIcon = remember {
        ImageVector.Builder(
            name = "Crown",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color(0xFFB8B6D5))) {
            moveTo(2f, 18f)
            lineTo(22f, 18f)
            lineTo(20f, 8f)
            lineTo(15f, 13f)
            lineTo(12f, 5f)
            lineTo(9f, 13f)
            lineTo(4f, 8f)
            close()
        }.build()
    }

    // Custom User Switch Icon SVG path
    val userSwitchIcon = remember {
        ImageVector.Builder(
            name = "UserSwitch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color(0xFFDE3B75))) {
            // Silhouette outline with arrows
            moveTo(10.25f, 8.25f)
            curveTo(10.25f, 6.73f, 11.48f, 5.5f, 13f, 5.5f)
            curveTo(14.52f, 5.5f, 15.75f, 6.73f, 15.75f, 8.25f)
            curveTo(15.75f, 9.77f, 14.52f, 11f, 13f, 11f)
            curveTo(11.48f, 11f, 10.25f, 9.77f, 10.25f, 8.25f)
            close()
            moveTo(13f, 12.25f)
            curveTo(10.24f, 12.25f, 7.5f, 13.62f, 7.5f, 16f)
            lineTo(18.5f, 16f)
            curveTo(18.5f, 13.62f, 15.76f, 12.25f, 13f, 12.25f)
            close()
            // Right-pointing & Left-pointing Arrows representing switch
            moveTo(7f, 8.5f)
            lineTo(4f, 11.5f)
            lineTo(7f, 14.5f)
            lineTo(7f, 12f)
            lineTo(10f, 12f)
            lineTo(10f, 11f)
            lineTo(7f, 11f)
            close()
        }.build()
    }

    if (showDowngradeDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDowngradeDialog = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141419))
                    .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                // Close button at top right
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clickable { showDowngradeDialog = false }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular user switch badge with pink border and subtle glow
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E1225).copy(alpha = 0.4f))
                            .border(1.dp, Color(0xFFDE3B75), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = userSwitchIcon,
                            contentDescription = null,
                            tint = Color(0xFFDE3B75),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Switch to User Mode?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "You will lose your speaker privileges and earnings potential. Admin approval is required to switch back. Are you sure?",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Keep account warning info card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2E1225).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDE3B75).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = Color(0xFFDE3B75),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "You'll still keep your account, data and conversations.",
                            color = Color(0xFFB8B6D5),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Side-by-side action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showDowngradeDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A35)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                onRequestDowngrade()
                                showDowngradeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE3B75)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1.8f).height(46.dp)
                        ) {
                            Text(
                                text = "Request Downgrade",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)) // Dark background #09090B
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 1. Premium Hero Card with diagonal gradient and bottom accent glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.dp) // Card height: 190-210dp
                .offset(y = cardOffsetY)
                .alpha(cardAlpha)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2A1438), // Deep Purple #2A1438
                            Color(0xFF0F1020)  // Dark Navy #0F1020
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp)) // 8% opacity border
        ) {
            // Subtle Pink Glow overlay near bottom-left for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFDE3B75).copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(100f, 500f),
                            radius = 350f
                        )
                    )
            )

            // Bottom horizontal glow fading into transparency
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFDE3B75).copy(alpha = 0.12f))
                        )
                    )
            )

            // Floating 48dp circular edit button on top-right with margin 18dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)) // Dark translucent background
                    .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.4f), CircleShape) // Subtle pink glow border
                    .clickable { onEditProfile(user.gender) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Aligned horizontal axis layout containing Avatar & Details
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp), // Card edges horizontal & vertical padding: 20dp
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with natural overlapping indicator (110dp x 110dp, 20dp rounded corners)
                val avatarResId = remember(user.avatarUrl) {
                    com.rkdevstudios.voxly.util.AvatarHelper.getDrawableId(context, user.avatarUrl)
                }

                Box(
                    modifier = Modifier.scale(avatarScale)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .size(110.dp)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFDE3B75), Color(0xFF8F00FF))
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        color = Color.Gray
                    ) {
                        if (avatarResId != 0) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Avatar",
                                modifier = Modifier.padding(24.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // Green online indicator overlapping naturally
                    if (user.isOnline) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp) // 20dp size
                                .background(Color(0xFF00FF66), CircleShape) // #00FF66 bright green
                                .border(2.5.dp, Color(0xFF0F1020), CircleShape) // Dark border matching navy backdrop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp)) // Spacing Avatar -> Details: 24dp

                // Details vertically centered
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Name: Bold, 36sp, negative letter spacing
                    Text(
                        text = user.displayName.ifEmpty { "Jessy" },
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp)) // Name -> Speaker: 8dp

                    // Speaker Badge with Custom Crown Icon and Muted Lavender text #B8B6D5
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // 8dp spacing between crown and text
                    ) {
                        Icon(
                            imageVector = crownIcon,
                            contentDescription = null,
                            tint = Color(0xFFB8B6D5)
                        )
                        Text(
                            text = "Speaker",
                            color = Color(0xFFB8B6D5),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp)) // Speaker -> Toggle: 18dp

                    // Custom Online Toggle Pill: Height 48dp, Width 170dp, 24dp radius
                    val thumbOffset by animateDpAsState(
                        targetValue = if (user.isOnline) 22.dp else 2.dp,
                        animationSpec = tween(durationMillis = 200)
                    )
                    Box(
                        modifier = Modifier
                            .size(170.dp, 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF130822)) // Very dark purple
                            .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.2f), RoundedCornerShape(24.dp)) // 20% opacity pink
                            .clickable {
                                android.util.Log.d("VoxlyToggle", "UI Toggle clicked: current isOnline = ${user.isOnline}, triggering onToggleOnline(${!user.isOnline})")
                                onToggleOnline(!user.isOnline)
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Online",
                                color = if (user.isOnline) Color(0xFF00FF66) else Color.Gray,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Custom switch toggle
                            Box(
                                modifier = Modifier
                                    .size(46.dp, 26.dp)
                                    .clip(CircleShape)
                                    .background(if (user.isOnline) Color(0xFFDE3B75) else Color(0xFF2A2A35))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = thumbOffset)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }

        var showPreview by remember { mutableStateOf(false) }

        if (showPreview) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showPreview = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    com.rkdevstudios.voxly.ui.components.ProfileCard(
                        name = user.displayName.ifEmpty { "Jessy" },
                        tags = user.tags,
                        avatarUrl = user.avatarUrl,
                        tagline = user.tagline,
                        availability = user.deriveAvailability(),
                        showActions = false
                    )
                }
            }
        }

        // Call Preferences Group
        Text(
            text = androidx.compose.ui.res.stringResource(id = com.rkdevstudios.voxly.R.string.call_preferences_label),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
            border = BorderStroke(1.dp, Color(0xFF2A2A35))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.rkdevstudios.voxly.R.string.incoming_calls_label),
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val currentTypes = user.callPreferences.getTypedSupportedCallTypes()
                val isAudioEnabled = currentTypes.contains(SupportedCallType.AUDIO)
                val isVideoEnabled = currentTypes.contains(SupportedCallType.VIDEO)

                // Audio Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Audio", color = Color.White, fontSize = 16.sp)
                    Switch(
                        checked = isAudioEnabled,
                        onCheckedChange = { checked ->
                            val newTypes = if (checked) {
                                (user.callPreferences.supportedCallTypes + "AUDIO").distinct()
                            } else {
                                user.callPreferences.supportedCallTypes - "AUDIO"
                            }
                            onUpdatePreferences(user.callPreferences.copy(supportedCallTypes = newTypes))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Video Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Video", color = Color.White, fontSize = 16.sp)
                    Switch(
                        checked = isVideoEnabled,
                        onCheckedChange = { checked ->
                            val newTypes = if (checked) {
                                (user.callPreferences.supportedCallTypes + "VIDEO").distinct()
                            } else {
                                user.callPreferences.supportedCallTypes - "VIDEO"
                            }
                            onUpdatePreferences(user.callPreferences.copy(supportedCallTypes = newTypes))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.rkdevstudios.voxly.R.string.call_preferences_disclaimer),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 2. First Menu Group (Card container with 20.dp rounded corners, dark background)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
            border = BorderStroke(1.dp, Color(0xFF2A2A35))
        ) {
            Column {
                SpeakerMenuItem(
                    icon = Icons.Rounded.Visibility,
                    iconTint = Color(0xFFDE3B75), // Pink
                    iconBg = Color(0xFF2A1520),
                    text = "Preview your Profile",
                    subtext = "See how others view your profile",
                    onClick = { showPreview = true }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)

                val tagsValue = user.tags.take(3).joinToString(", ") + if (user.tags.size > 3) "..." else ""
                SpeakerMenuItem(
                    icon = Icons.Rounded.Category,
                    iconTint = Color(0xFF8F00FF), // Purple
                    iconBg = Color(0xFF1C132B),
                    text = "Tags & Topics",
                    subtext = tagsValue.ifEmpty { "Music, Cinema, Travel and more" },
                    onClick = { onEditTags(user.avatarUrl) }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)

                val langValue = user.languages.joinToString(", ")
                SpeakerMenuItem(
                    icon = Icons.Rounded.Language,
                    iconTint = Color(0xFF42A5F5), // Blue
                    iconBg = Color(0xFF121A2E),
                    text = "Languages",
                    subtext = langValue.ifEmpty { "Telugu" },
                    onClick = {
                        val currentLang = user.languages.firstOrNull() ?: "English"
                        onEditLanguage(currentLang)
                    }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)

                val paymentStatus = if (user.paymentMethod.isNotEmpty()) "Linked (${user.paymentMethod})" else "Not Set"
                SpeakerMenuItem(
                    icon = Icons.Rounded.CreditCard,
                    iconTint = Color(0xFFFFB300), // Orange/Gold
                    iconBg = Color(0xFF2B2112),
                    text = "Payment Info",
                    subtext = paymentStatus,
                    onClick = onEditPayment
                )
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)

                SpeakerMenuItem(
                    icon = Icons.Rounded.Wallet,
                    iconTint = Color(0xFF4CAF50), // Green
                    iconBg = Color(0xFF112519),
                    text = "Earnings",
                    subtext = String.format(java.util.Locale.US, "₹%.2f", user.earnings),
                    onClick = onEarningsClick
                )
            }
        }

        // 3. Second Menu Group (Card container with 20.dp rounded corners, dark background)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
            border = BorderStroke(1.dp, Color(0xFF2A2A35))
        ) {
            Column {
                val downgradeText = if (user.downgradeRequest) "Downgrade Requested (Pending)" else "Switch to User Mode"
                SpeakerMenuItem(
                    icon = Icons.Rounded.SwapHoriz,
                    iconTint = Color(0xFF42A5F5), // Light blue
                    iconBg = Color(0xFF121F2B),
                    text = downgradeText,
                    subtext = "Change to user experience",
                    onClick = {
                        if (!user.downgradeRequest) {
                            showDowngradeDialog = true
                        }
                    }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)

                SpeakerMenuItem(
                    icon = Icons.Rounded.Settings,
                    iconTint = Color(0xFF9C27B0), // Violet
                    iconBg = Color(0xFF20132E),
                    text = "Account Settings",
                    subtext = "Manage your account",
                    onClick = onAccountSettingsClick
                )
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)

                SpeakerMenuItem(
                    icon = Icons.Rounded.Shield,
                    iconTint = Color(0xFFDE3B75), // Magenta
                    iconBg = Color(0xFF2E1225),
                    text = "Privacy & Safety",
                    subtext = "Control your privacy"
                )
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)

                SpeakerMenuItem(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    iconTint = Color(0xFFFFB300), // Orange
                    iconBg = Color(0xFF2B2012),
                    text = "Help & Support",
                    subtext = "Get help and support"
                )
                HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)

                SpeakerMenuItem(
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    iconTint = Color(0xFFE53935), // Red
                    iconBg = Color(0xFF2E1218),
                    text = "Log Out",
                    subtext = "Sign out from your account",
                    onClick = onLogout
                )
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
fun SpeakerMenuItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    text: String,
    subtext: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded Square Icon Container with subtle colors matching design specs
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            )
        }

        // Pink chevron right arrow matching the premium design spec
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFDE3B75),
            modifier = Modifier.size(20.dp)
        )
    }
}
