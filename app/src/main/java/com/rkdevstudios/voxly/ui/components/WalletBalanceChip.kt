package com.rkdevstudios.voxly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rkdevstudios.voxly.ui.theme.VoxlyDimens
import java.util.Locale

@Composable
fun WalletBalanceChip(
    coins: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF161618))
            .border(
                width = VoxlyDimens.BorderWidthThin,
                color = Color(0xFF2A2A35),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(
                start = VoxlyDimens.WalletPillPaddingStart,
                end = VoxlyDimens.WalletPillPaddingEnd,
                top = VoxlyDimens.WalletPillPaddingVertical,
                bottom = VoxlyDimens.WalletPillPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🪙",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = com.rkdevstudios.voxly.util.CoinFormatter.format(coins),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Coins",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
