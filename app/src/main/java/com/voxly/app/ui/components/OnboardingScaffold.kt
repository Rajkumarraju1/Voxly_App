package com.voxly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScaffold(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    topNavigationSlot: @Composable (() -> Unit)? = null,
    progressIndicatorSlot: @Composable (() -> Unit)? = null,
    headerSlot: @Composable (() -> Unit)? = null,
    bottomActionsSlot: @Composable (() -> Unit)? = null,
    bodyContent: @Composable (ColumnScope.() -> Unit)
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Consistent dark branding
            .navigationBarsPadding() // Safely handle navigation bar
            .statusBarsPadding() // Respect status bar padding
            .padding(horizontal = 24.dp)
    ) {
        // 1. Top Bar row containing navigation and progress slots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (topNavigationSlot != null) {
                topNavigationSlot()
            } else {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (progressIndicatorSlot != null) {
                Box(modifier = Modifier.weight(1f)) {
                    progressIndicatorSlot()
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Balancing space for back icon alignment if progress is centered
            Spacer(modifier = Modifier.width(40.dp))
        }

        // 2. Scrollable Body Content (including header slot)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            if (headerSlot != null) {
                headerSlot()
                Spacer(modifier = Modifier.height(16.dp))
            }
            bodyContent()
        }

        // 3. Bottom Actions Slot (Pushed to bottom and visible)
        if (bottomActionsSlot != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                bottomActionsSlot()
            }
        }
    }
}
