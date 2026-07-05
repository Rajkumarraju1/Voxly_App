package com.rkdevstudios.voxly.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rkdevstudios.voxly.util.AvatarHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rkdevstudios.voxly.ui.components.EditableProfileAvatar
import com.rkdevstudios.voxly.ui.components.VoxlyInputField
import com.rkdevstudios.voxly.ui.components.VoxlyPrimaryButton
import com.rkdevstudios.voxly.ui.theme.VoxlyDimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    currentName: String,
    currentTagline: String,
    currentTags: List<String>,
    currentAvatarUrl: String = "",
    gender: String = "",
    onUpdateProfile: suspend (String, String, List<String>) -> Unit,
    onBackClick: () -> Unit = {},
    onEditAvatarClick: (String) -> Unit = {}
) {
    var displayName by remember { mutableStateOf(currentName) }
    var tagline by remember { mutableStateOf(currentTagline) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Keep track of active focused states for the input fields
    var isNameFocused by remember { mutableStateOf(false) }
    var isTaglineFocused by remember { mutableStateOf(false) }
    
    // Expanded tag list
    val allTags = listOf(
        "Music", "Cinema", "Travel", "Deep talks", "Books", "Gaming", 
        "English practice", "Casual chat", "Fitness", "Cooking", 
        "Tech", "Art", "Politics", "Science", "History"
    )
    val selectedTags = remember { mutableStateListOf<String>().apply { addAll(currentTags) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Matches dark branding
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Update your details to stand out.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                
                EditableProfileAvatar(
                    avatarUrl = currentAvatarUrl,
                    onClick = { onEditAvatarClick(gender) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Display Name Input Field
            VoxlyInputField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display Name",
                placeholder = "Enter your display name",
                leadingIcon = Icons.Default.Person,
                characterLimit = 20,
                isError = displayName.isEmpty(),
                errorMessage = if (displayName.isEmpty()) "Display name cannot be empty" else null,
                readOnly = isLoading,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tagline Input Field
            VoxlyInputField(
                value = tagline,
                onValueChange = { tagline = it },
                label = "Tagline (Bio)",
                placeholder = "Tell people about yourself...",
                leadingIcon = Icons.Default.Edit,
                characterLimit = 100,
                readOnly = isLoading,
                singleLine = false
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Interests & Topics Section Title
            Text(
                text = "Interests & Topics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Select as many as you like.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Custom FlowRow Chips matching Home Screen tag style guidelines
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
                            if (!isLoading) {
                                if (isSelected) {
                                    selectedTags.remove(tag)
                                } else {
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

            Spacer(modifier = Modifier.height(32.dp))

            // Reusable Action Button
            VoxlyPrimaryButton(
                text = "Update Profile",
                onClick = { 
                    scope.launch {
                        isLoading = true
                        onUpdateProfile(displayName, tagline, selectedTags)
                        isLoading = false
                    }
                },
                enabled = displayName.isNotEmpty() && !isLoading,
                isLoading = isLoading,
                leadingIcon = Icons.Default.AutoAwesome,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
