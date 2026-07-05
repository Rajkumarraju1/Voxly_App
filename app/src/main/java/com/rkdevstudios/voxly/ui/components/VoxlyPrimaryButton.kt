package com.rkdevstudios.voxly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class VoxlyButtonVariant {
    PRIMARY,
    GOLD,
    SECONDARY,
    DESTRUCTIVE
}

@Composable
fun VoxlyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    variant: VoxlyButtonVariant = VoxlyButtonVariant.PRIMARY
) {
    // Click throttling helper
    var lastClickTime by remember { mutableStateOf(0L) }
    
    val colors = if (enabled && !isLoading) {
        when (variant) {
            VoxlyButtonVariant.PRIMARY -> listOf(Color(0xFFDE3B75), Color(0xFF8F00FF))
            VoxlyButtonVariant.GOLD -> listOf(Color(0xFFFFB300), Color(0xFFFF6F00))
            VoxlyButtonVariant.SECONDARY -> listOf(Color(0xFF2A2A35), Color(0xFF2A2A35))
            VoxlyButtonVariant.DESTRUCTIVE -> listOf(Color(0xFFFF3B30), Color(0xFFE02B20))
        }
    } else {
        listOf(Color(0xFF2A2A35), Color(0xFF2A2A35))
    }
    
    val gradient = Brush.horizontalGradient(colors = colors)

    Button(
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 1000L) { // 1 second throttle
                lastClickTime = currentTime
                onClick()
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFF2A2A35),
            contentColor = Color.White,
            disabledContentColor = Color(0xFF6B6B78)
        ),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) Color.White else Color(0xFF6B6B78)
                        )
                    )
                    
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
