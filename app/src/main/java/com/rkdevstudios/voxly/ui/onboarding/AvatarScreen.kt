package com.rkdevstudios.voxly.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rkdevstudios.voxly.ui.components.*
import com.rkdevstudios.voxly.util.AvatarHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarScreen(
    gender: String,
    onContinue: (String) -> Unit,
    onBack: () -> Unit = {},
    isOnboarding: Boolean = true
) {
    var selectedAvatar by remember { mutableStateOf<String?>(null) }
    
    val avatars = remember(gender) {
        if (gender.equals("Male", ignoreCase = true)) {
            AvatarHelper.getMaleAvatars()
        } else {
            AvatarHelper.getFemaleAvatars()
        }
    }

    OnboardingScaffold(
        onBackClick = onBack,
        progressIndicatorSlot = if (isOnboarding) {
            { OnboardingProgressIndicator(currentStep = 3, totalSteps = 5) }
        } else {
            null
        },
        headerSlot = {
            Column {
                Text(
                    text = "Choose your avatar",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select an avatar that represents you.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }
        },
        bottomActionsSlot = {
            VoxlyPrimaryButton(
                text = "Continue",
                onClick = { selectedAvatar?.let { onContinue(it) } },
                enabled = selectedAvatar != null,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    ) {
        val chunkedAvatars = remember(avatars) { avatars.chunked(2) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            chunkedAvatars.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { avatar ->
                        Box(modifier = Modifier.weight(1f)) {
                            AvatarOption(
                                avatarName = avatar,
                                isSelected = avatar == selectedAvatar,
                                onSelect = { selectedAvatar = avatar }
                            )
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarOption(
    avatarName: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val resId = remember(avatarName) {
        AvatarHelper.getDrawableId(context, avatarName)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(1.0f)
                .clip(CircleShape)
                .then(
                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier.border(1.dp, Color(0xFF2A2A35), CircleShape)
                )
                .padding(if (isSelected) 3.dp else 0.dp)
                .clip(CircleShape)
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = avatarName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .background(Color(0xFF1C1C22), CircleShape),
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(
                         imageVector = Icons.Default.Face,
                         contentDescription = null,
                         tint = Color.Gray
                     )
                 }
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
