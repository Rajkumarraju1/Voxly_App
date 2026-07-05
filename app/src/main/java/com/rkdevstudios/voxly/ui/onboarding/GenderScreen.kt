package com.rkdevstudios.voxly.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkdevstudios.voxly.ui.components.*

@Composable
fun GenderScreen(
    onContinue: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var selectedGender by remember { mutableStateOf<String?>(null) }
    val genders = listOf("Male", "Female")

    OnboardingScaffold(
        onBackClick = onBackClick,
        progressIndicatorSlot = {
            OnboardingProgressIndicator(
                currentStep = 2,
                totalSteps = 5
            )
        },
        headerSlot = {
            Column {
                Text(
                    text = buildAnnotatedString {
                        append("Tell us\nabout ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("you")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This helps us personalize your experience.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }
        },
        bottomActionsSlot = {
            VoxlyPrimaryButton(
                text = "Continue",
                onClick = { selectedGender?.let { onContinue(it) } },
                enabled = selectedGender != null,
                leadingIcon = Icons.Default.AutoAwesome,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            genders.forEach { gender ->
                val isSelected = gender == selectedGender
                
                SelectableCard(
                    title = gender,
                    isSelected = isSelected,
                    onClick = { selectedGender = gender },
                    isVertical = true,
                    modifier = Modifier.weight(1f),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (gender == "Male") Color(0xFF1E88E5).copy(alpha = 0.12f)
                                    else Color(0xFFDE3B75).copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (gender == "Male") "♂" else "♀",
                                fontSize = 32.sp,
                                color = if (gender == "Male") Color(0xFF1E88E5) else Color(0xFFDE3B75),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }
        }
    }
}
