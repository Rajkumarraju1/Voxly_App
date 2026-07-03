package com.voxly.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxly.app.ui.components.*
import com.voxly.app.ui.theme.VoxlyDimens
import com.voxly.app.util.AvatarHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    onSetupComplete: (String, List<String>) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var displayName by remember { mutableStateOf("") }
    val allTags = listOf(
        "Music", "Cinema", "Travel", "Deep talks", "Books", "Gaming", 
        "English practice", "Casual chat"
    )
    val selectedTags = remember { mutableStateListOf<String>() }

    OnboardingScaffold(
        onBackClick = onBackClick,
        progressIndicatorSlot = {
            OnboardingProgressIndicator(
                currentStep = 4,
                totalSteps = 5
            )
        },
        headerSlot = {
            Column {
                Text(
                    text = buildAnnotatedString {
                        append("Complete\nyour ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("profile")
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
                    text = "Add a few details to help people know you better.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }
        },
        bottomActionsSlot = {
            VoxlyPrimaryButton(
                text = "Finish Setup",
                onClick = { onSetupComplete(displayName, selectedTags) },
                enabled = displayName.isNotBlank() && selectedTags.isNotEmpty(),
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(8.dp))

            // Display Name Input
            VoxlyInputField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display Name",
                placeholder = "Enter display name",
                leadingIcon = Icons.Default.Person,
                characterLimit = 20,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Select Interests Section
            Text(
                text = "Select Interests (1-3)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = "Choose topics you enjoy",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Flow Row of Selectable Chips with Emoji mappings
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    val isSelected = selectedTags.contains(tag)
                    val emoji = AvatarHelper.getTagEmoji(tag)
                    
                    Surface(
                        onClick = {
                            if (isSelected) {
                                selectedTags.remove(tag)
                            } else {
                                if (selectedTags.size < 3) {
                                    selectedTags.add(tag)
                                }
                            }
                        },
                        shape = RoundedCornerShape(VoxlyDimens.TagCornerRadius),
                        border = if (isSelected) null else BorderStroke(VoxlyDimens.BorderWidthThin, Color(0xFF2A2A35)),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF141419),
                        modifier = Modifier.height(VoxlyDimens.ChipHeight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$emoji $tag",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(VoxlyDimens.SmallIconSize)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
