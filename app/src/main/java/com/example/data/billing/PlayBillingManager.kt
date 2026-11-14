package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BillingUiState {
    data object Connecting : BillingUiState
    data class Ready(val products: Map<String, ProductDetails>) : BillingUiState
    data class Unavailable(val reason: String) : BillingUiState
    data class PurchaseUpdated(val message: String) : BillingUiState
}

class PlayBillingManager(context: Context) : PurchasesUpdatedListener {

    private val _state = MutableStateFlow<BillingUiState>(BillingUiState.Connecting)
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private var productDetailsMap: Map<String, ProductDetails> = emptyMap()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            querySubscriptionProducts()
            return
        }
        _state.value = BillingUiState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscriptionProducts()
                } else {
                    _state.value = BillingUiState.Unavailable(
                        "Google Play Billing unavailable (${result.debugMessage.ifBlank { "code ${result.responseCode}" }})."
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = BillingUiState.Unavailable("Google Play Billing service disconnected.")
            }
        })
    }

    private fun querySubscriptionProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_FAMILY_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, detailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsMap = detailsList.associateBy { it.productId }
                if (productDetailsMap.isEmpty()) {
                    _state.value = BillingUiState.Unavailable(
                        "Subscription SKUs are not published in Google Play Console yet. Essential safety tools remain free."
                    )
                } else {
                    _state.value = BillingUiState.Ready(productDetailsMap)
                }
            } else {
                _state.value = BillingUiState.Unavailable(
                    "Unable to fetch Play Store plans: ${billingResult.debugMessage}"
                )
            }
        }
    }

    fun launchSubscriptionFlow(activity: Activity, productId: String): String? {
        val details = productDetailsMap[productId]
            ?: return "Subscription product '$productId' is not yet configured in Google Play Console."
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return "No active subscription offer found for '$productId'."

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) null
        else "Could not launch Play Billing flow: ${result.debugMessage}"
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                _state.value = BillingUiState.PurchaseUpdated(
                    "Purchase received. Server-side Play verification will activate your plan once verified."
                )
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.value = BillingUiState.Ready(productDetailsMap)
            }
            else -> {
                _state.value = BillingUiState.Unavailable(
                    "Billing error: ${result.debugMessage.ifBlank { "code ${result.responseCode}" }}"
                )
            }
        }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    companion object {
        const val PRODUCT_PREMIUM_MONTHLY = "scamshield_premium_monthly"
        const val PRODUCT_FAMILY_MONTHLY = "scamshield_family_monthly"
    }
}
