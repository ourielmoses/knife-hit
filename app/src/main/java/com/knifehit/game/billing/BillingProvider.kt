package com.knifehit.game.billing

import com.knifehit.game.model.CurrencyPack

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult
    data object Unavailable : PurchaseResult
    data class Failed(val reason: String) : PurchaseResult
}

interface BillingProvider {
    suspend fun purchase(pack: CurrencyPack): PurchaseResult
}

object NoopBillingProvider : BillingProvider {
    override suspend fun purchase(pack: CurrencyPack): PurchaseResult = PurchaseResult.Unavailable
}
