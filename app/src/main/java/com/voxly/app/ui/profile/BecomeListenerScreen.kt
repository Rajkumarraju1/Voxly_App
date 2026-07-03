package com.voxly.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BecomeListenerScreen(
    verificationStatus: String,
    onBackClick: () -> Unit,
    onStartEarningClick: () -> Unit,
    onGoToDashboard: () -> Unit // Added callback
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(verificationStatus) {
                            "pending" -> "Application Status"
                            "verified" -> "Congratulations!"
                            else -> "Become a Listener"
                        }
                    ) 
                },
                navigationIcon = {
                    if (verificationStatus != "verified") {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (verificationStatus == "verified") {
                // Verified State UI
                Spacer(modifier = Modifier.height(32.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = com.voxly.app.ui.theme.PremiumGold // Or primary color
                )
                Text(
                    text = "Welcome to the Team!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your application has been approved. You are now a verified listener on Voxly.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                     modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                         Text(
                            text = "Next Steps:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                         )
                         Spacer(modifier = Modifier.height(8.dp))
                         Text(
                            text = "• Go to your dashboard to set your availability.\n• Complete your profile to attract more callers.\n• Start earning money for every minute you talk!",
                            style = MaterialTheme.typography.bodyMedium
                         )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                 Button(
                    onClick = onGoToDashboard,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Go to Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

            } else if (verificationStatus == "pending") {
                // Pending State UI Redesigned
                
                // 1. Large Clipboard Status Header
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow effect
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFDE3B75).copy(alpha = 0.2f), Color.Transparent)
                                )
                            )
                    )
                    // Inner Circle
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.4f), CircleShape)
                            .background(Color(0xFF100F14)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Clipboard FactCheck Icon in pink/purple gradient
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFDE3B75), Color(0xFF8F00FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title and description
                Row {
                    Text(
                        text = "Application ",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Submitted!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE91E63)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Thank you for applying! Our team is reviewing your profile.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Timeline Card Section ("What happens next?")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDE3B75).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = Color(0xFFDE3B75),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "What happens next?",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Timeline Rows
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Row 1
                            TimelineItem(
                                stepNumber = "01",
                                title = "We verify your details",
                                desc = "within 24 hours.",
                                icon = Icons.Default.Shield,
                                isLast = false
                            )
                            // Row 2
                            TimelineItem(
                                stepNumber = "02",
                                title = "You will receive a notification",
                                desc = "once approved.",
                                icon = Icons.Default.Notifications,
                                isLast = false
                            )
                            // Row 3
                            TimelineItem(
                                stepNumber = "03",
                                title = "Start earning immediately",
                                desc = "after approval.",
                                icon = Icons.Default.AttachMoney, // Using AttachMoney for coin/rupee icon
                                isLast = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Boarding Excited Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14101F)) // Dark purple tint
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8F00FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFA500),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "We're excited to have you on board!",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Get ready to connect, chat and earn.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        // Unicode sparkles
                        Text("✦", color = Color(0xFFDE3B75), fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Action Button: Go Back Home
                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Go Back Home",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Sales UI (Redesigned matching screenshot)
                
                // 1. Hero Section Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coin Image/Icon in gold circle
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Row {
                                Text(
                                    text = "Turn Your Time ",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Row {
                                Text(
                                    text = "Into ",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Money",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF53860) // Pink/red highlight
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Listen, chat and earn real cash instantly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Call Rate Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Audio Rate Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141013))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E1A1A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color(0xFFFC7A1E)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Audio Call", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "₹2.25", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(text = "per min", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2A1F13))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "📈 +33% more", color = Color(0xFFFFA500), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Video Rate Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFF8F00FF).copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF110D1A))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1430)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Video Call", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "₹6.75", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(text = "per min", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C132E))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "📈 +42% more", color = Color(0xFFC084FC), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Potential Earnings Table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Potential Earnings",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Table Headers with Icons instead of Audio/Video text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Duration", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.2f))
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color(0xFFFC7A1E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "₹2.25/m", color = Color(0xFFFC7A1E), fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "₹6.75/m", color = Color(0xFFC084FC), fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.8.dp)
                        
                        val durations = listOf("10 min", "20 min", "1 hr", "2 hrs")
                        val audios = listOf("₹22.50", "₹45.00", "₹135.00", "₹270.00")
                        val videos = listOf("₹67.50", "₹135.00", "₹405.00", "₹810.00")
                        
                        durations.forEachIndexed { i, duration ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = duration, color = Color.White, fontSize = 14.sp)
                                }
                                Text(text = audios[i], color = Color(0xFFFFA500), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                Text(text = videos[i], color = Color(0xFFE91E63), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF2A2A35), thickness = 0.5.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Estimated Daily Earnings Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE91E63), Color(0xFFFC7A1E))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = "Estimated Daily Earnings",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹1,080/day",
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            
                            // Visual sum calculations
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Audio (2h)", 
                                        color = Color.White.copy(alpha = 0.8f), 
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "₹270", 
                                        color = Color.White, 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "+", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Video (2h)", 
                                        color = Color.White.copy(alpha = 0.8f), 
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "₹810", 
                                        color = Color.White, 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Based on 2 hours of audio calls and 2 hours of video calls. Actual earnings depend on completed call duration.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Why Become a Listener? Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Why Become a Listener?",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WhyCard(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconBg = Color(0xFF1B3D2F),
                            iconTint = Color(0xFF4CAF50),
                            title = "Earn Real Cash",
                            desc = "Get paid for every minute you talk or listen.",
                            modifier = Modifier.weight(1f)
                        )
                        WhyCard(
                            icon = Icons.Default.Schedule,
                            iconBg = Color(0xFF2D1B4E),
                            iconTint = Color(0xFF9C27B0),
                            title = "Work Anytime",
                            desc = "Choose your own time. Work from anywhere.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WhyCard(
                            icon = Icons.Default.People,
                            iconBg = Color(0xFF1A2F4C),
                            iconTint = Color(0xFF2196F3),
                            title = "Meet New People",
                            desc = "Have meaningful conversations with amazing people.",
                            modifier = Modifier.weight(1f)
                        )
                        WhyCard(
                            icon = Icons.Default.Shield,
                            iconBg = Color(0xFF3E2D1A),
                            iconTint = Color(0xFFFF9800),
                            title = "100% Secure",
                            desc = "We protect your privacy and data.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 6. Become a Listener Action Button
                Button(
                    onClick = onStartEarningClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(24.dp)) // Center text somewhat
                        Text(
                            text = "Become a Listener",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Safe • Trusted • Transparent
                Text(
                    text = "🔒 Safe • Trusted • Transparent",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WhyCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, color = Color.Gray, fontSize = 10.sp, lineHeight = 12.sp)
        }
    }
}

@Composable
fun TimelineItem(
    stepNumber: String,
    title: String,
    desc: String,
    icon: ImageVector,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline indicator (Left side)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            // Circle node
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF100F14))
                    .border(2.dp, Color(0xFFDE3B75), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDE3B75))
                )
            }
            if (!isLast) {
                // Vertical dashed connector
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(
                            color = Color(0xFFDE3B75).copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Center Step Number Circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1B21)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color(0xFFDE3B75),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        // Right side icon container
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1B21)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (icon == Icons.Default.AttachMoney) Color(0xFFFFA500) else Color(0xFFDE3B75),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
