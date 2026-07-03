package com.voxly.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
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
import com.voxly.app.ui.components.*

@Composable
fun LanguageScreen(
    initialSelection: String? = null,
    onContinue: (String) -> Unit,
    onBackClick: () -> Unit = {},
    isOnboarding: Boolean = true
) {
    val languages = listOf("English", "Hindi", "Telugu", "Tamil", "Kannada", "Malayalam")
    var selectedLanguage by remember { mutableStateOf(initialSelection) }

    OnboardingScaffold(
        onBackClick = onBackClick,
        progressIndicatorSlot = if (isOnboarding) {
            { OnboardingProgressIndicator(currentStep = 1, totalSteps = 5) }
        } else {
            null
        },
        headerSlot = {
            Column {
                Text(
                    text = buildAnnotatedString {
                        append("Choose your\n")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("preferred language")
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
                    text = "You’ll be matched with people who speak the same language.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }
        },
        bottomActionsSlot = {
            VoxlyPrimaryButton(
                text = "Continue",
                onClick = { selectedLanguage?.let { onContinue(it) } },
                enabled = selectedLanguage != null,
                leadingIcon = Icons.Default.AutoAwesome,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val chunkedLanguages = remember(languages) { languages.chunked(2) }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                chunkedLanguages.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { language ->
                            val isSelected = language == selectedLanguage
                            
                            Box(modifier = Modifier.weight(1f)) {
                                SelectableCard(
                                    title = language,
                                    isSelected = isSelected,
                                    onClick = { selectedLanguage = language },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else Color(0xFF1E1E22)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (language) {
                                                "English" -> Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                "Hindi" -> Text(
                                                    text = "ह",
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                "Telugu" -> Text(
                                                    text = "అ",
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                "Tamil" -> Text(
                                                    text = "அ",
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                "Kannada" -> Text(
                                                    text = "ಕ",
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                "Malayalam" -> Text(
                                                    text = "മ",
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            VoxlyInfoBanner(
                description = "You can change this later in settings",
                icon = Icons.Default.Security,
                style = BannerStyle.INFO
            )
        }
    }
}
