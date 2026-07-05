package com.rkdevstudios.voxly.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..totalSteps) {
            val isCompleted = i <= currentStep
            val dotColor by animateColorAsState(
                targetValue = if (isCompleted) MaterialTheme.colorScheme.primary else Color(0xFF2A2A35),
                animationSpec = tween(durationMillis = 300),
                label = "DotColorAnimation"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            if (i < totalSteps) {
                val isLineCompleted = i < currentStep
                val lineColor by animateColorAsState(
                    targetValue = if (isLineCompleted) MaterialTheme.colorScheme.primary else Color(0xFF2A2A35),
                    animationSpec = tween(durationMillis = 300),
                    label = "LineColorAnimation"
                )

                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(2.dp)
                        .background(lineColor)
                )
            }
        }
    }
}
