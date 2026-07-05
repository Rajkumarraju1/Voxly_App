package com.rkdevstudios.voxly.ui.call

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionSummaryScreen(
    duration: String,
    earnings: String, // Value (e.g. ₹10.00)
    isSpeaker: Boolean,
    onRate: (Int) -> Unit = {},
    onDone: () -> Unit
) {
    // Custom Diamond Sparkle Icon
    val sparkleIcon = remember {
        ImageVector.Builder(
            name = "Sparkle",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color(0xFFDE3B75))) {
            moveTo(12f, 2f)
            quadTo(12f, 12f, 22f, 12f)
            quadTo(12f, 12f, 12f, 22f)
            quadTo(12f, 12f, 2f, 12f)
            quadTo(12f, 12f, 12f, 2f)
            close()
        }.build()
    }

    Surface(
        color = Color(0xFF09090B), // Deep premium black
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Success badge with sparkles
            Box(
                modifier = Modifier
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow rings
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                        .border(3.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), CircleShape)
                )
                
                // Solid green check circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF09090B),
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Surrounding sparkles
                // Sparkle top-right (pink)
                Icon(
                    imageVector = sparkleIcon,
                    contentDescription = null,
                    tint = Color(0xFFDE3B75),
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-16).dp, y = 12.dp)
                )

                // Sparkle mid-left (green)
                Icon(
                    imageVector = sparkleIcon,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 10.dp, y = (-15).dp)
                )

                // Sparkle bottom-left (green)
                Icon(
                    imageVector = sparkleIcon,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50).copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = 24.dp, y = (-12).dp)
                )

                // Sparkle top-center (green)
                Icon(
                    imageVector = sparkleIcon,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50).copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopCenter)
                        .offset(x = 36.dp, y = 8.dp)
                )
            }

            // 2. Titles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Session Completed!",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Great conversation! 💗",
                    color = Color(0xFFB8B6D5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // 3. Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFDE3B75).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDE3B75).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFFDE3B75),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Session Duration", 
                                color = Color.White, 
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = duration, 
                            color = Color.White, 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isSpeaker) {
                        HorizontalDivider(color = Color(0xFF2A2A35))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "₹",
                                        color = Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Earnings", 
                                    color = Color.White, 
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = earnings,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Rating Section (Only for matching user, not speaker)
            if (!isSpeaker) {
                var rating by remember { mutableIntStateOf(0) }
                var submitted by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!submitted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Rate your experience", 
                                color = Color.White, 
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your feedback helps us improve", 
                                color = Color.Gray, 
                                fontSize = 13.sp
                            )
                        }

                        // Star selector row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { index ->
                                val isSelected = index <= rating
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star $index",
                                    tint = if (isSelected) Color(0xFFFFB300) else Color(0xFFFFB300).copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clickable { rating = index }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Rating Button
                        Button(
                            onClick = { 
                                if (rating > 0) {
                                    onRate(rating) 
                                    submitted = true
                                }
                            },
                            enabled = rating > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDE3B75),
                                disabledContainerColor = Color(0xFFDE3B75).copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .width(220.dp)
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submit Rating", 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Thanks for rating! 🌟", 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5. Divider with pink heart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2A2A35))
                Text(
                    text = " ♥ ", 
                    color = Color(0xFFDE3B75), 
                    fontSize = 14.sp, 
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2A2A35))
            }

            // 6. Back to Home Button
            OutlinedButton(
                onClick = onDone,
                border = BorderStroke(1.dp, Color(0xFFDE3B75)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDE3B75).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            tint = Color(0xFFDE3B75),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Back to Home", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
