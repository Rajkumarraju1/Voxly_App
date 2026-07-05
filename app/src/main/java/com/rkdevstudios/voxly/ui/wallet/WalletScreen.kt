package com.rkdevstudios.voxly.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rkdevstudios.voxly.data.model.Transaction
import com.rkdevstudios.voxly.ui.viewmodel.HomeViewModel
import com.rkdevstudios.voxly.util.CoinConstants
import com.rkdevstudios.voxly.util.CoinFormatter
import com.rkdevstudios.voxly.util.findActivity
import java.text.SimpleDateFormat
import java.util.Locale

data class WalletPack(
    val productId: String,
    val badge: String?,
    val highlight: Boolean,
    val displayOrder: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val coinProducts by viewModel.coinProducts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isTxLoading by viewModel.isTransactionLoading.collectAsState()

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Infinite Scroll for transaction history
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= transactions.size + 4 && !isTxLoading) {
                    viewModel.loadMoreTransactions()
                }
            }
    }

    val walletPacks = remember {
        listOf(
            WalletPack("coins_100", "Starter", highlight = false, displayOrder = 1),
            WalletPack("coins_250", "Value", highlight = false, displayOrder = 2),
            WalletPack("coins_500", "Popular", highlight = true, displayOrder = 3),
            WalletPack("coins_1000", "⭐ Best Value", highlight = true, displayOrder = 4),
            WalletPack("coins_2500", "Premium", highlight = false, displayOrder = 5),
            WalletPack("coins_5000", "VIP", highlight = false, displayOrder = 6)
        ).sortedBy { it.displayOrder }
    }

    fun initiatePurchase(productDetails: com.android.billingclient.api.ProductDetails) {
        val activity = context.findActivity()
        if (activity != null) {
            viewModel.launchBillingFlow(activity, productDetails)
        } else {
            android.widget.Toast.makeText(
                context,
                "Cannot start payment: Activity not found",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF09090B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09090B))
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Premium Balance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFDE3B75), Color(0xFF9333EA))
                                )
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = "CURRENT BALANCE",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🪙", fontSize = 30.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = CoinFormatter.format(currentUser?.coins),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Section Header: Buy Coins
            item {
                Text(
                    text = "Buy Coins",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 2. Coin Packs Layout
            val chunkedPacks = walletPacks.chunked(2)
            chunkedPacks.forEach { rowPacks ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPacks.forEach { pack ->
                            val productDetails = coinProducts.find { it.productId == pack.productId }
                            val coinsGranted = CoinConstants.PRODUCT_ID_TO_COINS[pack.productId] ?: 0
                            val audioMinutes = coinsGranted / 10
                            val videoMinutes = coinsGranted / 30

                            Box(modifier = Modifier.weight(1f)) {
                                CoinPackCard(
                                    pack = pack,
                                    coinsGranted = coinsGranted,
                                    audioMinutes = audioMinutes,
                                    videoMinutes = videoMinutes,
                                    productDetails = productDetails,
                                    isProductsLoading = coinProducts.isEmpty(),
                                    onClick = {
                                        productDetails?.let { initiatePurchase(it) }
                                    }
                                )
                            }
                        }
                        if (rowPacks.size < 2) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 3. Future Placeholders
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Offers & Rewards",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PromoPlaceholderCard(title = "First Purchase", desc = "+50% Coins", modifier = Modifier.weight(1f))
                        PromoPlaceholderCard(title = "Referral Reward", desc = "Earn Free Coins", modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PromoPlaceholderCard(title = "Promo Code", desc = "Redeem Coupon", modifier = Modifier.weight(1f))
                        PromoPlaceholderCard(title = "Festival Offers", desc = "Coming Soon", modifier = Modifier.weight(1f))
                    }
                }
            }

            // 4. Transaction History Header
            item {
                Text(
                    text = "Transaction History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTxLoading) "Loading history..." else "No transactions yet.",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(transactions) { transaction ->
                    WalletTransactionItem(transaction)
                }

                if (isTxLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoinPackCard(
    pack: WalletPack,
    coinsGranted: Int,
    audioMinutes: Int,
    videoMinutes: Int,
    productDetails: com.android.billingclient.api.ProductDetails?,
    isProductsLoading: Boolean,
    onClick: () -> Unit
) {
    val isEnabled = productDetails != null
    val borderStroke = if (pack.highlight) {
        BorderStroke(2.dp, Brush.horizontalGradient(listOf(Color(0xFFDE3B75), Color(0xFF9333EA))))
    } else {
        BorderStroke(1.dp, Color(0xFF272730))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isEnabled) { onClick() },
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131316))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge text if available
            if (pack.badge != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (pack.highlight) Color(0xFFDE3B75) else Color(0xFF27273A),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = pack.badge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Coin Amount
            Text(
                text = "$coinsGranted Coins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Estimations
            Text(
                text = "≈$audioMinutes min Audio\n≈$videoMinutes min Video",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Prices (Play console formattedPrice)
            val buttonText = when {
                isProductsLoading -> "Loading..."
                productDetails != null -> productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: "Price Error"
                else -> "Unavailable"
            }

            Button(
                onClick = onClick,
                enabled = isEnabled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pack.highlight) Color(0xFF9333EA) else Color(0xFF272730),
                    disabledContainerColor = Color(0xFF1A1A1E)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isEnabled) Color.White else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun PromoPlaceholderCard(
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(72.dp),
        border = BorderStroke(1.dp, Color(0xFF1F1F23)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun WalletTransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
        border = BorderStroke(1.dp, Color(0xFF1E1E22))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "Coin Purchase" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (transaction.timestamp != null) {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    Text(
                        text = dateFormat.format(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isDebit = transaction.amount < 0
                Text(
                    text = if (isDebit) "-${transaction.coins} Coins" else "+${transaction.coins} Coins",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDebit) Color(0xFFEF5350) else Color(0xFF66BB6A)
                )
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.2f", java.lang.Math.abs(transaction.amount))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}
