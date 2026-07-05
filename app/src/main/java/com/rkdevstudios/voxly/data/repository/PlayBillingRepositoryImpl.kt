package com.rkdevstudios.voxly.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayBillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayBillingRepository, PurchasesUpdatedListener {

    private val TAG = "PlayBillingRepo"
    private val _coinProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    override val coinProducts: StateFlow<List<ProductDetails>> = _coinProducts.asStateFlow()

    private val _purchaseEvents = MutableStateFlow<PurchaseEvent?>(null)
    override val purchaseEvents: StateFlow<PurchaseEvent?> = _purchaseEvents.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    // Replace with your actual product IDs from Play Console (loaded from centralized configurations)
    private val consumableProductIds = com.rkdevstudios.voxly.util.CoinConstants.PRODUCT_ID_TO_COINS.keys.toList()

    override fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Client Connected")
                    queryProductDetails()
                } else {
                    Log.e(TAG, "Billing Client Connection Failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing Client Disconnected. Reconnecting...")
                startConnection()
            }
        })
    }

    private fun queryProductDetails() {
        val productList = consumableProductIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _coinProducts.value = productDetailsList
            } else {
                Log.e(TAG, "Error querying products: ${billingResult.debugMessage}")
            }
        }
    }

    override fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _purchaseEvents.value = PurchaseEvent.Success(purchase)
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseEvents.value = PurchaseEvent.Error("Purchase cancelled by user")
        } else {
            _purchaseEvents.value = PurchaseEvent.Error("Purchase failed: ${billingResult.debugMessage}")
        }
    }

    override suspend fun verifyAndConsumePurchase(purchase: Purchase): Boolean {
        // Ideally, send purchase.purchaseToken to your secure backend (Firebase Functions) here
        // For MVP/Testing before backend is ready, we consume client-side directly:
        
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        return try {
            val consumeResult = billingClient.consumePurchase(consumeParams)
            consumeResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK
        } catch (e: Exception) {
            Log.e(TAG, "Error consuming purchase", e)
            false
        }
    }

    override fun clearPurchaseEvent() {
        _purchaseEvents.value = null
    }
}
