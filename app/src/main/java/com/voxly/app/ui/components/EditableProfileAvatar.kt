package com.voxly.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.voxly.app.util.AvatarHelper

@Composable
fun EditableProfileAvatar(
    avatarUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 80.dp
) {
    val context = LocalContext.current
    val resId = remember(avatarUrl) {
        AvatarHelper.getDrawableId(context, avatarUrl)
    }

    Box(
        modifier = modifier
            .size(size + 8.dp) // Extra padding to avoid clipping the badge on the edges
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Main Avatar Circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .background(Color(0xFF1C1C22))
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Avatar Placeholder",
                        tint = Color.Gray,
                        modifier = Modifier.size(size / 2)
                    )
                }
            }
        }

        // Edit Badge (Pencil icon) overlay on bottom right
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.BottomEnd)
                .padding(bottom = 2.dp, end = 2.dp) // Shift slightly inward to avoid being clipped
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Avatar",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
