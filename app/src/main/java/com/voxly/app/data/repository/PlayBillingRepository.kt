package com.voxly.app.data.repository

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface PlayBillingRepository {
    val coinProducts: StateFlow<List<com.android.billingclient.api.ProductDetails>>
    val purchaseEvents: StateFlow<PurchaseEvent?>
    
    fun startConnection()
    fun launchBillingFlow(activity: Activity, productDetails: com.android.billingclient.api.ProductDetails)
    suspend fun verifyAndConsumePurchase(purchase: com.android.billingclient.api.Purchase): Boolean
    fun clearPurchaseEvent()
}

sealed class PurchaseEvent {
    data class Success(val purchase: com.android.billingclient.api.Purchase) : PurchaseEvent()
    data class Error(val message: String) : PurchaseEvent()
}
