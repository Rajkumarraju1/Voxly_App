package com.voxly.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.voxly.app.data.model.User
import com.voxly.app.data.model.Call
import com.voxly.app.data.model.SpeakerAvailabilityUi
import com.voxly.app.data.model.SpeakerStatus
import com.voxly.app.data.model.deriveAvailability
import com.voxly.app.ui.components.SpeakerCallActions
import com.voxly.app.ui.components.getStatusColor
import com.voxly.app.ui.speaker.EarningsScreen
import com.voxly.app.ui.viewmodel.HistoryViewModel
import com.voxly.app.ui.viewmodel.HomeViewModel
import com.voxly.app.util.AvatarHelper
import com.voxly.app.util.DateHelper
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    onWalletClick: () -> Unit = {}
) {
    val currentUser by homeViewModel.currentUser.collectAsState()
    val history by historyViewModel.history.collectAsState()
    val transactions by homeViewModel.transactions.collectAsState()
    val isTransactionLoading by homeViewModel.isTransactionLoading.collectAsState()
    val isHistoryLoading by historyViewModel.isLoading.collectAsState()

    currentUser?.let { user ->
        if (user.isSpeaker) {
            EarningsScreen(
                user = user, 
                history = history,
                transactions = transactions,
                isTransactionLoading = isTransactionLoading,
                isHistoryLoading = isHistoryLoading,
                onLoadMoreTransactions = { homeViewModel.loadMoreTransactions() },
                onLoadMoreHistory = { historyViewModel.loadMore() },
                onWithdraw = { amount, method, details ->
                    homeViewModel.submitWithdrawal(amount, method, details)
                }
            )
        } else {
             HistoryListContent(
                 history = history, currentUser = user, isLoading = isHistoryLoading,
                 homeViewModel = homeViewModel,
                 historyViewModel = historyViewModel,
                 onWalletClick = onWalletClick
             )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryListContent(
    history: List<Call>,
    currentUser: User,
    isLoading: Boolean,
    homeViewModel: HomeViewModel,
    historyViewModel: HistoryViewModel,
    onWalletClick: () -> Unit
) {
    val groupedHistory = remember(history) {
        history.groupBy { call ->
            DateHelper.formatHistoryDate(call.createdAt)
        }
    }
    
    val activeSpeakers by homeViewModel.speakers.collectAsState()
    val isSelectionMode by historyViewModel.isSelectionMode.collectAsState()
    val selectedItems by historyViewModel.selectedItems.collectAsState()
    
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        Dialog(onDismissRequest = { showClearDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glow trash icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow circle
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
                        // Inner circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFDE3B75), Color(0xFF8F00FF))
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Clear History",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "What would you like to do?",
                        color = Color.Gray,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Option 1: Clear All
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showClearDialog = false
                                historyViewModel.clearAllHistory()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B21))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular icon container
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDE3B75).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFDE3B75),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear All",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Remove all your session history permanently.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFDE3B75)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: Select & Delete
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showClearDialog = false
                                historyViewModel.toggleSelectionMode()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B21))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular icon container
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8F00FF).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CropFree,
                                    contentDescription = null,
                                    tint = Color(0xFF8F00FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Select & Delete",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Choose specific sessions to delete.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF8F00FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Cancel button
                    Button(
                        onClick = { showClearDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(100.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Matches premium dark mode background
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { historyViewModel.toggleSelectionMode() }) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                    Text(
                        text = "${selectedItems.size} Selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = { historyViewModel.deleteSelectedItems() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Selected",
                        tint = Color(0xFFE53935)
                    )
                }
            } else {
                Text(
                    text = "Sessions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 28.sp
                    )
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Trash Delete Button inside rounded card shape
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF141419))
                            .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(10.dp))
                            .clickable { showClearDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Clear History",
                            tint = Color(0xFFDE3B75),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Coins Chip Indicator
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFF141419),
                        border = BorderStroke(1.dp, Color(0xFF2A2A35)),
                        modifier = Modifier
                            .height(38.dp)
                            .clickable(onClick = onWalletClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentUser.coins}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
        
        if (isLoading && history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No session history", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 110.dp // Lift above floating navigation overlay
                )
            ) {
                groupedHistory.forEach { (dateHeader, calls) ->
                    // Date Header Centered Pill
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFF141419),
                                border = BorderStroke(1.dp, Color(0xFF2A2A35)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dateHeader,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    
                    // Session Items
                    items(calls) { call ->
                        val otherId = if (call.callerId == currentUser.id) call.speakerId else call.callerId
                        val matchedSpeaker = activeSpeakers.firstOrNull { it.id == otherId }
                        val isStale = matchedSpeaker?.let { (System.currentTimeMillis() - it.lastSeen) > 120000L } ?: true
                        val isOnline = (matchedSpeaker?.isOnline ?: false) && !isStale
                        val isBusy = matchedSpeaker?.activeCall != null
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                           if (isSelectionMode) {
                               Checkbox(
                                   checked = selectedItems.contains(call.id),
                                   onCheckedChange = { historyViewModel.toggleSelection(call.id) },
                                   colors = CheckboxDefaults.colors(
                                       checkedColor = MaterialTheme.colorScheme.primary,
                                       uncheckedColor = Color.Gray
                                   ),
                                   modifier = Modifier.padding(start = 16.dp)
                               )
                           }
                           
                           Box(modifier = Modifier
                               .weight(1f)
                               .combinedClickable(
                                   onClick = { 
                                       if (isSelectionMode) {
                                           historyViewModel.toggleSelection(call.id)
                                       }
                                   },
                                   onLongClick = {
                                       if (!isSelectionMode) {
                                           historyViewModel.toggleSelectionMode()
                                           historyViewModel.toggleSelection(call.id)
                                       }
                                   }
                               )
                           ) {
                                  val availability = matchedSpeaker?.deriveAvailability() ?: SpeakerAvailabilityUi(SpeakerStatus.OFFLINE, false, false)
                                  SessionHistoryItem(
                                      call = call, 
                                      currentUserId = currentUser.id,
                                      availability = availability,
                                      onCall = { if (!isSelectionMode) homeViewModel.initiateSmartRedial(otherId, "audio") },
                                      onVideoCall = { if (!isSelectionMode) homeViewModel.initiateSmartRedial(otherId, "video") }
                                  )
                           }
                        }
                    }
                }
                
                // Trigger pagination
                item {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            historyViewModel.loadMore()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionHistoryItem(
    call: Call, 
    currentUserId: String,
    availability: SpeakerAvailabilityUi,
    onCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    val isCaller = call.callerId == currentUserId
    val displayName = if (isCaller) call.speakerName else call.callerName
    val displayAvatar = if (isCaller) call.speakerAvatar else call.callerAvatar
    
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeStr = call.createdAt?.let { dateFormat.format(it) } ?: ""
    
    val durationMinutes = call.duration / 60
    val durationStr = if (durationMinutes > 0) String.format("%02d min", durationMinutes) else "${call.duration} sec"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
        border = BorderStroke(1.dp, Color(0xFF2A2A35))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Dot status overlay indicator
            val context = LocalContext.current
            val avatarResId = AvatarHelper.getDrawableId(context, displayAvatar)
            
            Box {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    color = Color.Gray
                ) {
                    if (avatarResId != 0) {
                        Image(
                           painter = painterResource(id = avatarResId),
                           contentDescription = null,
                           contentScale = ContentScale.Crop
                       )
                    } else {
                         Icon(
                             imageVector = Icons.Default.Person,
                             contentDescription = null,
                             modifier = Modifier.padding(12.dp),
                             tint = Color.White
                         )
                    }
                }
                
                // Status dot color mapping using unified component status colors
                val dotColor = availability.status.getStatusColor()
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .background(dotColor, CircleShape)
                        .border(2.5.dp, Color(0xFF141419), CircleShape) // Seamless circular border cut
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Small Red/Pink tinted clock/time label
                    Text(
                        text = "🕒 $timeStr  •  $durationStr",
                        color = Color(0xFFDE3B75),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Call Action Buttons using unified component in compact mode
            SpeakerCallActions(
                availability = availability,
                audioRate = 10,
                videoRate = 30,
                showPricing = false,
                compactLayout = true,
                onCallClick = onCall,
                onVideoCallClick = onVideoCall
            )
        }
    }
}
