package com.knifehit.game.ads

sealed interface AdResult {
    data object Completed : AdResult
    data object Dismissed : AdResult
    data object Failed : AdResult
}

enum class AdKind { INTERSTITIAL, REWARDED }

interface AdProvider {
    suspend fun showInterstitial(): AdResult
    suspend fun showRewarded(): AdResult
}

class MockAdProvider : AdProvider {
    override suspend fun showInterstitial(): AdResult = AdResult.Completed
    override suspend fun showRewarded(): AdResult = AdResult.Completed
}
