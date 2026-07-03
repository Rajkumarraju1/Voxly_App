package com.voxly.app.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voxly.app.data.model.CoinPack
import com.voxly.app.data.model.PackCategory
import com.voxly.app.ui.viewmodel.HomeViewModel
import com.voxly.app.util.findActivity
import kotlinx.coroutines.launch

data class CoinPackUI(
    val pack: CoinPack,
    val originalPrice: String? = null,
    val badgeText: String? = null,
    val discountPillText: String? = null,
    val emoji: String,
    val productDetails: com.android.billingclient.api.ProductDetails? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val coins = currentUser?.coins?.toInt() ?: 0
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val coinProducts by viewModel.coinProducts.collectAsState()

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

    // Map Google Play Products to our UI representation
    val uiPacks = rememberCoinPacksUI(coinProducts)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Current balance pill top right
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF27272A),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = coins.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF09090B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09090B))
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(uiPacks) { uiPack ->
                CoinGridItem(
                    uiPack = uiPack,
                    onClick = { 
                        if (uiPack.productDetails != null) {
                            initiatePurchase(uiPack.productDetails) 
                        } else {
                            android.widget.Toast.makeText(context, "This package is not yet available in Google Play Console.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item(span = { GridItemSpan(3) }) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CoinGridItem(uiPack: CoinPackUI, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Main Background Box with clip
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF131316))
                .border(
                    width = 1.dp,
                    color = Color(0xFF272730),
                    shape = RoundedCornerShape(16.dp)
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 26.dp, bottom = if (uiPack.discountPillText != null) 34.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Emoji Icon
            Text(
                text = uiPack.emoji,
                fontSize = 34.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Coin Amount
            Text(
                text = uiPack.pack.coins.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Pricing Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiPack.originalPrice != null) {
                    Text(
                        text = uiPack.originalPrice,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF71717A),
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                
                val displayPrice = uiPack.productDetails?.oneTimePurchaseOfferDetails?.formattedPrice 
                    ?: "₹${uiPack.pack.price}"

                Text(
                    text = displayPrice,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        // Top Badge (Overlapping border)
        if (uiPack.badgeText != null) {
             Box(
                 modifier = Modifier
                     .align(Alignment.TopCenter)
                     .offset(y = (-8).dp)
                     .background(Color(0xFF09090B)) // hide border line
                     .padding(horizontal = 8.dp)
             ) {
                 Text(
                     text = uiPack.badgeText,
                     style = MaterialTheme.typography.labelSmall,
                     color = if (uiPack.badgeText.contains("Value")) Color(0xFFFFB020) else Color(0xFFFF3366),
                     fontWeight = FontWeight.ExtraBold,
                 )
             }
        }

        // Bottom Discount Pill (Vibrant Gradient)
        if (uiPack.discountPillText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Apply clipping just to the pill background so it matches the box curve at bottom
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF9333EA), Color(0xFF4C1D95))
                        )
                    )
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiPack.discountPillText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF3E8FF),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun rememberCoinPacksUI(playProducts: List<com.android.billingclient.api.ProductDetails>): List<CoinPackUI> {
    
    fun findProduct(id: String): com.android.billingclient.api.ProductDetails? {
        return playProducts.find { it.productId == id }
    }

    return listOf(
        // We only show packs if they are available from Google Play, 
        // to prevent users clicking null products before loading completes.
        CoinPackUI(
            pack = CoinPack(80, 59, 59.0 / 80.0, category = PackCategory.SMALL),
            emoji = "🪙💰",
            productDetails = findProduct("coins_80")
        ),
        CoinPackUI(
            pack = CoinPack(300, 149, 149.0 / 300.0, category = PackCategory.SMALL),
            emoji = "🪙💰",
            productDetails = findProduct("coins_300")
        ),
        CoinPackUI(
            pack = CoinPack(450, 251, 251.0 / 450.0, category = PackCategory.SMALL),
            emoji = "✨💰",
            productDetails = findProduct("coins_450")
        ),
        CoinPackUI(
            pack = CoinPack(1100, 550, 550.0 / 1100.0, category = PackCategory.MEDIUM),
            emoji = "🪙🪙",
            productDetails = findProduct("coins_1100")
        ),
        CoinPackUI(
            pack = CoinPack(1800, 1055, 1055.0 / 1800.0, category = PackCategory.MEDIUM),
            emoji = "🛒💰",
            productDetails = findProduct("coins_1800")
        ),
        CoinPackUI(
            pack = CoinPack(3500, 1599, 1599.0 / 3500.0, category = PackCategory.LARGE),
            discountPillText = "Flat ₹500 OFF",
            emoji = "🧺💰",
            productDetails = findProduct("coins_3500")
        ),
        CoinPackUI(
            pack = CoinPack(5000, 599, 599.0 / 5000.0, category = PackCategory.LARGE),
            badgeText = "🔥 Hot",
            originalPrice = "₹1999",
            emoji = "🏛️💰",
            productDetails = findProduct("coins_5000")
        ),
        CoinPackUI(
            pack = CoinPack(9000, 2651, 2651.0 / 9000.0, category = PackCategory.MEGA),
            badgeText = "🔥 Hot",
            originalPrice = "₹3251",
            emoji = "💼💰",
            productDetails = findProduct("coins_9000")
        ),
        CoinPackUI(
            pack = CoinPack(20000, 5000, 5000.0 / 20000.0, category = PackCategory.MEGA),
            badgeText = "👑 VALUE PACK",
            originalPrice = "₹8000",
            discountPillText = "Flat ₹3000 OFF",
            emoji = "🎁👑",
            productDetails = findProduct("coins_20000")
        )
    )
}
