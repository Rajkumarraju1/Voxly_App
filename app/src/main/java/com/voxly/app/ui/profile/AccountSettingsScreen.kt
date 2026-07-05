package com.voxly.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxly.app.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    user: User,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    var pushEnabled by remember { mutableStateOf(true) }
    var emailEnabled by remember { mutableStateOf(true) }
    
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Scaffold(
        containerColor = Color(0xFF0A0A0A), // Clean black background
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Account Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 1. Notifications Section
            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    icon = Icons.Rounded.Notifications,
                    title = "Push Notifications",
                    subtitle = "Receive alerts and updates",
                    checked = pushEnabled,
                    iconTint = Color(0xFFDE3B75),
                    onCheckedChange = { pushEnabled = it }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSwitchItem(
                    icon = Icons.Rounded.Email,
                    title = "Email Updates",
                    subtitle = "Important news and offers",
                    checked = emailEnabled,
                    iconTint = Color(0xFFDE3B75),
                    onCheckedChange = { emailEnabled = it }
                )
            }
            
            // 2. Legal Section
            SettingsSection(title = "Legal") {
                SettingsItem(
                    icon = Icons.Rounded.Lock,
                    title = "Privacy Policy",
                    subtitle = "Learn how we protect your data",
                    iconTint = Color(0xFF9C47FF),
                    onClick = { uriHandler.openUri("https://voxly.vibeme.live/privacy") }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Rounded.Description,
                    title = "Terms of Service",
                    subtitle = "Rules and guidelines",
                    iconTint = Color(0xFF9C47FF),
                    onClick = { uriHandler.openUri("https://voxly.vibeme.live/terms") }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = "Refund Policy",
                    subtitle = "Returns and refunds",
                    iconTint = Color(0xFF9C47FF),
                    onClick = { uriHandler.openUri("https://voxly.vibeme.live/refunds") }
                )
            }

            // 3. Support & Info
            SettingsSection(title = "Support & Info") {
                SettingsItem(
                    icon = Icons.Rounded.SupportAgent,
                    title = "Help & Support",
                    subtitle = "Get help with your queries",
                    iconTint = Color(0xFFB060FF),
                    onClick = { uriHandler.openUri("https://voxly.vibeme.live/help") }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Rounded.Description,
                    title = "Engineering Blog",
                    subtitle = "Updates from our team",
                    iconTint = Color(0xFFB060FF),
                    onClick = { uriHandler.openUri("https://voxly.vibeme.live/blog") }
                )
            }

            // 4. Danger Zone
            SettingsSection(title = "Danger Zone", titleColor = Color(0xFFE53935)) {
                SettingsItem(
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    title = "Log Out",
                    subtitle = "Sign out from your account",
                    isLogout = true,
                    onClick = { showLogoutDialog = true }
                )
                HorizontalDivider(color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Rounded.DeleteForever,
                    title = "Delete Account",
                    subtitle = "Permanently remove your data",
                    isLogout = true,
                    textColor = Color(0xFFE53935),
                    onClick = { showDeleteDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.Gray,
                    fontSize = 13.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", color = Color.White) },
            text = { Text("Are you sure you want to log out?", color = Color.Gray) },
            containerColor = Color(0xFF141419),
            confirmButton = {
                TextButton(onClick = { 
                    showLogoutDialog = false
                    onLogout() 
                }) {
                    Text("Log Out", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showDeleteDialog) {
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glow Trash & Warning Badge Illustration
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow circle
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFFE53935).copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                        )
                        // Inner circle with gradient
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFDE3B75), Color(0xFF8B0000))
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Warning Triangle Overlapping Symbol at bottom right
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF100F14))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300), // Amber/Yellow alert color
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Delete Account",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This action cannot be undone. All your data, including coins and history, will be permanently lost.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Button
                        Button(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Delete Forever Button
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                onDeleteAccount()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Delete Forever",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    titleColor: Color = Color(0xFFDE3B75),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = titleColor,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(start = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141419))
                .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = Color(0xFFB060FF),
    isLogout: Boolean = false,
    textColor: Color = Color.White,
    onClick: () -> Unit = {}
) {
    val containerShape = RoundedCornerShape(16.dp)
    val containerModifier = Modifier
        .size(52.dp)
        .clip(containerShape)
        .then(
            if (isLogout) {
                Modifier
                    .background(Color(0xFFE53935).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.2f), containerShape)
            } else {
                Modifier
                    .background(Color(0xFF1E1D26))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), containerShape)
            }
        )

    val currentIconTint = if (isLogout) Color(0xFFE53935) else iconTint

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
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
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

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    iconTint: Color = Color(0xFFDE3B75),
    onCheckedChange: (Boolean) -> Unit
) {
    val containerShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(containerShape)
                .background(Color(0xFF1E1D26))
                .border(1.dp, Color.White.copy(alpha = 0.05f), containerShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
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

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFDE3B75),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2A2A35),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
