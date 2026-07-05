package com.rkdevstudios.voxly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class BannerStyle {
    INFO,
    SUCCESS,
    WARNING
}

@Composable
fun VoxlyInfoBanner(
    description: String,
    modifier: Modifier = Modifier,
    style: BannerStyle = BannerStyle.INFO,
    icon: ImageVector? = null,
    title: String? = null
) {
    val themeColor = when (style) {
        BannerStyle.INFO -> MaterialTheme.colorScheme.primary
        BannerStyle.SUCCESS -> Color(0xFF4CAF50)
        BannerStyle.WARNING -> Color(0xFFFF9800)
    }

    val bannerBgColor = themeColor.copy(alpha = 0.08f)

    val fallbackIcon = when (style) {
        BannerStyle.INFO -> Icons.Default.Info
        BannerStyle.SUCCESS -> Icons.Default.CheckCircle
        BannerStyle.WARNING -> Icons.Default.Warning
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bannerBgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon ?: fallbackIcon,
            contentDescription = null,
            tint = themeColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
        }
    }
}
