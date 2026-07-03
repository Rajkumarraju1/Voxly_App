package com.voxly.app.ui.speaker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxly.app.data.model.User
import com.voxly.app.data.model.Call
import com.voxly.app.data.model.Transaction
import com.voxly.app.util.AvatarHelper
import com.voxly.app.util.DateHelper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun EarningsScreen(
    user: User,
    history: List<Call> = emptyList(),
    transactions: List<Transaction> = emptyList(),
    onWithdraw: (Double, String, String) -> Unit = { _, _, _ -> },
    onLoadMoreTransactions: () -> Unit = {},
    isTransactionLoading: Boolean = false,
    onLoadMoreHistory: () -> Unit = {},
    isHistoryLoading: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sessions, 1 = Transactions
    var selectedFilter by rememberSaveable { mutableStateOf(DashboardFilter.ALL) }

    // Pure presentation-layer derived states
    val filteredHistory = remember(history, selectedFilter) {
        filterSessions(history, selectedFilter)
    }

    val filteredTransactions = remember(transactions, selectedFilter) {
        filterTransactions(transactions, selectedFilter)
    }

    val groupedHistory = remember(filteredHistory) {
        filteredHistory.groupBy { call ->
            DateHelper.formatHistoryDate(call.createdAt)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Premium dark background matching history screen
            .then(if (onBackClick != null) Modifier.statusBarsPadding() else Modifier) // Prevent overlap only when standalone
    ) {
        // Header Row matching mockup (Title + Top-right action button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp, 
                    end = 16.dp, 
                    top = if (onBackClick != null) 12.dp else 4.dp, 
                    bottom = 24.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "Earnings Dashboard",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 28.sp
                ),
                modifier = Modifier.weight(1f)
            )

            // Wallet/Card Icon Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141419))
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = "Wallet",
                    tint = Color(0xFFDE3B75),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Wrap the rest of the contents in a padded column with weight(1f)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Hero Total Earnings Card (Premium design with pink-to-purple diagonal gradient, radial highlight and 60/40 layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp) // Consistent height for visual impact
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF8F00FF), // Deep Purple
                            Color(0xFFDE3B75)  // Rich Magenta/Pink
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        ) {
            // Subtle Radial highlight effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                            radius = 500f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), // Generous internal padding (24dp)
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Total Earnings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp)) // Improved typography vertical rhythm
                    val earningsValue = user.earnings
                    val formattedEarnings = String.format(Locale.getDefault(), "₹%.2f", earningsValue)
                    
                    // Dominant earnings text element
                    Text(
                        text = formattedEarnings,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 40.sp,
                            letterSpacing = (-1).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp)) // Improved typography vertical rhythm
                    Text(
                        text = "Available to Withdraw: $formattedEarnings",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 24dp spacing between major sections

        // 2. Primary Withdraw Button (Full-width, 56dp height, pill shape, gold gradient)
        Button(
            onClick = { showWithdrawDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Height around 56dp
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp), // Large pill radius
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD54F), // Bright gold
                                Color(0xFFFFB300)  // Gold
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Withdraw Earnings",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Info container Card for Minimum withdrawal note
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2A2A35).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141419).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color(0xFFDE3B75),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Minimum withdrawal amount: ₹500",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 24dp spacing between major sections

        // 3. Custom Tab Design matching aesthetics (Custom Row to avoid TabRow indicator overlay issues)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141419))
                .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(14.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sessions Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 0) Color(0xFF1E1D26) else Color.Transparent)
                    .border(
                        1.dp,
                        if (selectedTab == 0) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selectedTab == 0) Color(0xFFDE3B75) else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sessions",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) Color(0xFFDE3B75) else Color.Gray
                    )
                }
            }

            // Transactions Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 1) Color(0xFF1E1D26) else Color.Transparent)
                    .border(
                        1.dp,
                        if (selectedTab == 1) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selectedTab == 1) Color(0xFFDE3B75) else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transactions",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) Color(0xFFDE3B75) else Color.Gray
                    )
                }
            }
        }

        // 4. Elegant Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp), // Better spacing
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardFilter.values().forEach { filterType ->
                    val filterLabel = when (filterType) {
                        DashboardFilter.ALL -> "All"
                        DashboardFilter.TODAY -> "Today"
                        DashboardFilter.THIS_WEEK -> "This Week"
                        DashboardFilter.THIS_MONTH -> "This Month"
                    }
                    val isSelected = selectedFilter == filterType
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isSelected) Color(0xFFDE3B75).copy(alpha = 0.12f) else Color(0xFF141419),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFDE3B75).copy(alpha = 0.6f) else Color(0xFF2A2A35)
                        ),
                        modifier = Modifier
                            .height(36.dp) // Consistent height (36dp)
                            .clickable { selectedFilter = filterType }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 18.dp), // Larger horizontal padding
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filterLabel,
                                color = if (isSelected) Color(0xFFDE3B75) else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Auto-load transactions when tab is selected
        LaunchedEffect(selectedTab) {
            if (selectedTab == 1 && transactions.isEmpty() && !isTransactionLoading) {
                onLoadMoreTransactions()
            }
        }

        // Lists Container
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                // Sessions History Tab
                if (history.isEmpty() && !isHistoryLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recent sessions", color = Color.Gray, fontSize = 16.sp)
                    }
                } else if (filteredHistory.isEmpty() && !isHistoryLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sessions found for this period.", color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    val historyListState = androidx.compose.foundation.lazy.rememberLazyListState()

                    // Infinite Scroll
                    LaunchedEffect(historyListState) {
                        androidx.compose.runtime.snapshotFlow { historyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { lastIndex ->
                                if (lastIndex != null && lastIndex >= history.size - 2 && !isHistoryLoading) {
                                    onLoadMoreHistory()
                                }
                            }
                    }

                    LazyColumn(
                        state = historyListState,
                        verticalArrangement = Arrangement.spacedBy(16.dp), // 16dp spacing between cards
                        contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp)
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

                            items(calls) { call ->
                                EarningsSessionItem(call)
                            }
                        }

                        if (isHistoryLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFDE3B75))
                                }
                            }
                        }
                    }
                }
            } else {
                // Transactions History Tab
                if (transactions.isEmpty() && !isTransactionLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No transactions yet", color = Color.Gray, fontSize = 16.sp)
                    }
                } else if (filteredTransactions.isEmpty() && !isTransactionLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No transactions found for this period.", color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                    // Infinite Scroll
                    LaunchedEffect(listState) {
                        androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { lastIndex ->
                                if (lastIndex != null && lastIndex >= transactions.size - 2 && !isTransactionLoading) {
                                    onLoadMoreTransactions()
                                }
                            }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(16.dp), // 16dp spacing between cards
                        contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp)
                    ) {
                        items(transactions) { transaction ->
                            TransactionItem(transaction)
                        }

                        if (isTransactionLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFDE3B75))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showWithdrawDialog) {
            WithdrawDialog(
                user = user,
                onDismiss = { showWithdrawDialog = false },
                onSubmit = { amount, method, details ->
                    onWithdraw(amount, method, details)
                    showWithdrawDialog = false
                }
            )
        }
        } // Close nested Column
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    val isCredit = transaction.amount >= 0
    val color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFE53935)
    val prefix = if (isCredit) "+" else ""

    val dateStr = transaction.timestamp?.let {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Processing..."

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), // Rounded corners (20-24dp)
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
        border = BorderStroke(1.dp, Color(0xFF2A2A35)) // Soft border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // Comfortable internal padding (20dp)
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "Transaction" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                if (transaction.status == "pending") {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.voxly.app.ui.theme.RichGold
                    )
                } else if (transaction.status == "failed") {
                    Text(
                        text = "Rejected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE53935)
                    )
                } else if (transaction.status == "success") {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Text(
                text = "$prefix₹${String.format(Locale.getDefault(), "%.2f", Math.abs(transaction.amount))}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

fun deriveBankFromIfsc(ifsc: String): Pair<String, String> {
    if (ifsc.length < 4) return Pair("", "")
    val prefix = ifsc.substring(0, 4).uppercase()
    val bankName = when (prefix) {
        "SBIN" -> "State Bank of India"
        "ICIC" -> "ICICI Bank"
        "HDFC" -> "HDFC Bank"
        "BARB" -> "Bank of Baroda"
        "UTIB" -> "Axis Bank"
        "PUNB" -> "Punjab National Bank"
        "CNRB" -> "Canara Bank"
        "KKBK" -> "Kotak Mahindra Bank"
        "YESB" -> "Yes Bank"
        "IBKL" -> "IDBI Bank"
        "IDIB" -> "Indian Bank"
        "UCOB" -> "UCO Bank"
        "UBIN" -> "Union Bank of India"
        "IOBA" -> "Indian Overseas Bank"
        "JAKA" -> "Jammu & Kashmir Bank"
        "MAHB" -> "Bank of Maharashtra"
        "PSIB" -> "Punjab & Sind Bank"
        "DLXB" -> "Dhanlaxmi Bank"
        "TMBL" -> "Tamilnad Mercantile Bank"
        "KVBL" -> "Karur Vysya Bank"
        "FSFB" -> "Fincare Small Finance Bank"
        "PAYT" -> "Paytm Payments Bank"
        "AIRP" -> "Airtel Payments Bank"
        else -> "Unknown Bank"
    }
    val branch = if (ifsc.length >= 11) "Branch Code: ${ifsc.substring(5)}" else "Main Branch"
    return Pair(bankName, branch)
}

@Composable
fun WithdrawDialog(
    user: User,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String) -> Unit
) {
    val context = LocalContext.current

    val formattedValue = remember(user.earnings) {
        String.format(Locale.getDefault(), "%.2f", user.earnings)
    }
    val formattedDisplay = remember(user.earnings) {
        String.format(Locale.getDefault(), "₹%.2f", user.earnings)
    }

    var amountText by remember { mutableStateOf(formattedValue) }
    var method by remember {
        mutableStateOf(if (user.paymentMethod.isNotEmpty()) user.paymentMethod else "UPI")
    }
    var upiId by remember { mutableStateOf(user.upiId) }
    var bankAccountNumber by remember { mutableStateOf(user.bankAccountNumber) }
    var bankIfscCode by remember { mutableStateOf(user.ifscCode) }

    // Custom Double Arrow Icon (>>) for UPI selection
    val doubleArrowIcon = remember {
        androidx.compose.ui.graphics.vector.ImageVector.Builder(
            name = "DoubleArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = androidx.compose.ui.graphics.SolidColor(Color(0xFFDE3B75))) {
            moveTo(6.41f, 6f)
            lineTo(5f, 7.41f)
            lineTo(9.58f, 12f)
            lineTo(5f, 16.59f)
            lineTo(6.41f, 18f)
            lineTo(12.41f, 12f)
            close()
            moveTo(13f, 6f)
            lineTo(11.59f, 7.41f)
            lineTo(16.17f, 12f)
            lineTo(11.59f, 16.59f)
            lineTo(13f, 18f)
            lineTo(19f, 12f)
            close()
        }.build()
    }

    // Render as a Custom Bottom-Aligned Sheet Overlay Dialog
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() } // Dismiss on outer click
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center)
                    .clickable(enabled = false) {} // Prevent click propagation
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141419))
                    .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Wallet Badge with pink border and glow
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E1225).copy(alpha = 0.4f))
                                .border(1.dp, Color(0xFFDE3B75), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFFDE3B75),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Withdraw Earnings",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total Available: $formattedDisplay",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Input 1: Amount field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Amount",
                            color = Color(0xFFB8B6D5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            placeholder = { Text("0.00", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF09090B),
                                unfocusedContainerColor = Color(0xFF09090B),
                                focusedBorderColor = Color(0xFFDE3B75),
                                unfocusedBorderColor = Color(0xFF2A2A35)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Input 2: Method Selector Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // UPI Tab Card
                        val isUpi = method == "UPI"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isUpi) Color(0xFF2E1225).copy(alpha = 0.3f) else Color(0xFF09090B))
                                .clickable { method = "UPI" }
                                .border(
                                    1.dp,
                                    if (isUpi) Color(0xFFDE3B75) else Color(0xFF2A2A35),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = doubleArrowIcon,
                                    contentDescription = null,
                                    tint = if (isUpi) Color(0xFFDE3B75) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "UPI",
                                    color = if (isUpi) Color(0xFFDE3B75) else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Bank Tab Card
                        val isBank = method == "Bank"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isBank) Color(0xFF2E1225).copy(alpha = 0.3f) else Color(0xFF09090B))
                                .clickable { method = "Bank" }
                                .border(
                                    1.dp,
                                    if (isBank) Color(0xFFDE3B75) else Color(0xFF2A2A35),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (isBank) Color(0xFFDE3B75) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bank Transfer",
                                    color = if (isBank) Color(0xFFDE3B75) else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Input 3: Details fields based on selected method
                    if (method == "UPI") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "UPI ID",
                                color = Color(0xFFB8B6D5),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                placeholder = { Text("Enter your UPI ID", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF09090B),
                                    unfocusedContainerColor = Color(0xFF09090B),
                                    focusedBorderColor = Color(0xFFDE3B75),
                                    unfocusedBorderColor = Color(0xFF2A2A35)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "You will receive the amount on this UPI ID",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Account Number field
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Account Number",
                                    color = Color(0xFFB8B6D5),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = bankAccountNumber,
                                    onValueChange = { bankAccountNumber = it },
                                    placeholder = { Text("Enter account number", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF09090B),
                                        unfocusedContainerColor = Color(0xFF09090B),
                                        focusedBorderColor = Color(0xFFDE3B75),
                                        unfocusedBorderColor = Color(0xFF2A2A35)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // IFSC Code field
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "IFSC Code",
                                    color = Color(0xFFB8B6D5),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = bankIfscCode,
                                    onValueChange = { bankIfscCode = it.uppercase() },
                                    placeholder = { Text("Enter IFSC code", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF09090B),
                                        unfocusedContainerColor = Color(0xFF09090B),
                                        focusedBorderColor = Color(0xFFDE3B75),
                                        unfocusedBorderColor = Color(0xFF2A2A35)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Dynamic read-only bank info card
                            val derivedBankDetails = remember(bankIfscCode) { deriveBankFromIfsc(bankIfscCode) }
                            val derivedBankName = derivedBankDetails.first
                            val derivedBranchName = derivedBankDetails.second

                            if (derivedBankName.isNotEmpty() && derivedBankName != "Unknown Bank") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF09090B))
                                        .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDE3B75).copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = Color(0xFFDE3B75),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = derivedBankName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = derivedBranchName,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions: Cancel & Request buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A35)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                val detailsToSubmit = if (method == "UPI") upiId else "$bankAccountNumber ($bankIfscCode)"
                                
                                if (detailsToSubmit.trim().isEmpty() || (method == "Bank" && (bankAccountNumber.isEmpty() || bankIfscCode.isEmpty()))) {
                                    android.widget.Toast.makeText(context, "Please enter payment details", android.widget.Toast.LENGTH_SHORT).show()
                                } else if (amount < 500) {
                                    android.widget.Toast.makeText(context, "Minimum withdrawal amount is ₹500", android.widget.Toast.LENGTH_SHORT).show()
                                } else if (amount > user.earnings) {
                                    android.widget.Toast.makeText(context, "Insufficient balance", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onSubmit(amount, method, detailsToSubmit)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE3B75)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1.3f).height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Request Payout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsSessionItem(call: Call) {
    val callerName = call.callerName
    val earningsValue = call.speakerEarned
    
    // Amount styling rules:
    // ₹0.00 -> Muted gold
    // Positive non-zero earnings -> Bright gold
    // Negative earnings (if any) -> Soft red
    val (earningsText, amountColor) = when {
        earningsValue > 0.0 -> {
            val formatted = String.format(Locale.getDefault(), "₹%.2f", earningsValue)
            Pair(formatted, Color(0xFFFFB300)) // Bright gold matching amount styling
        }
        earningsValue < 0.0 -> {
            val formatted = String.format(Locale.getDefault(), "-₹%.2f", Math.abs(earningsValue))
            Pair(formatted, Color(0xFFEF9A9A)) // Soft red
        }
        else -> {
            Pair("₹0.00", Color(0xFF8D6E63)) // Muted gold
        }
    }

    val durationStr = if (call.duration > 60) {
        "${call.duration / 60}m ${call.duration % 60}s"
    } else {
        "${call.duration}s"
    }

    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeStr = call.createdAt?.let { dateFormat.format(it) } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), // Rounded corners (20-24dp)
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
        border = BorderStroke(1.dp, Color(0xFF2A2A35)) // Soft borders
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // Comfortable internal padding (20dp)
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with natural overlapping green dot indicator
            val context = LocalContext.current
            val avatarResId = AvatarHelper.getDrawableId(context, call.callerAvatar)

            Box {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp), // Avatar size 60dp
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
                
                // Simple overlapping online indicator green dot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                        .border(2.5.dp, Color(0xFF141419), CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Center Column: Name & Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callerName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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

            // Right side Amount (vertically centered with caller name)
            Text(
                text = earningsText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = amountColor
            )
        }
    }
}

enum class DashboardFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

fun filterSessions(history: List<Call>, filter: DashboardFilter): List<Call> {
    if (filter == DashboardFilter.ALL) return history
    
    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayDay = now.get(Calendar.DAY_OF_YEAR)
    val todayWeek = now.get(Calendar.WEEK_OF_YEAR)
    val todayMonth = now.get(Calendar.MONTH)

    return history.filter { call ->
        val createdAt = call.createdAt ?: return@filter false
        val cal = Calendar.getInstance().apply { time = createdAt }
        val callYear = cal.get(Calendar.YEAR)
        
        when (filter) {
            DashboardFilter.TODAY -> {
                callYear == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay
            }
            DashboardFilter.THIS_WEEK -> {
                callYear == todayYear && cal.get(Calendar.WEEK_OF_YEAR) == todayWeek
            }
            DashboardFilter.THIS_MONTH -> {
                callYear == todayYear && cal.get(Calendar.MONTH) == todayMonth
            }
            else -> true
        }
    }
}

fun filterTransactions(transactions: List<Transaction>, filter: DashboardFilter): List<Transaction> {
    if (filter == DashboardFilter.ALL) return transactions
    
    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayDay = now.get(Calendar.DAY_OF_YEAR)
    val todayWeek = now.get(Calendar.WEEK_OF_YEAR)
    val todayMonth = now.get(Calendar.MONTH)

    return transactions.filter { transaction ->
        val timestamp = transaction.timestamp ?: return@filter false
        val cal = Calendar.getInstance().apply { time = timestamp }
        val transYear = cal.get(Calendar.YEAR)
        
        when (filter) {
            DashboardFilter.TODAY -> {
                transYear == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay
            }
            DashboardFilter.THIS_WEEK -> {
                transYear == todayYear && cal.get(Calendar.WEEK_OF_YEAR) == todayWeek
            }
            DashboardFilter.THIS_MONTH -> {
                transYear == todayYear && cal.get(Calendar.MONTH) == todayMonth
            }
            else -> true
        }
    }
}
