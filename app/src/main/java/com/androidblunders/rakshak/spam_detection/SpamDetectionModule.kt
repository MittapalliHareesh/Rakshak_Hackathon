package com.androidblunders.rakshak.spam_detection

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module that wires the Spam Detection pipeline.
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  HOW TO ADD A NEW ANALYZER                                          │
 * │  1. Create a class implementing [ThreatAnalyzer] (@Inject ctor).     │
 * │  2. Add it to the list returned by [provideAnalyzers] below.        │
 * │  ThreatFusionEngine picks it up automatically (weights applied by    │
 * │  list position: index 0 = local, index 1 = cloud).                  │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Current registered analyzers:
 *  • [GemmaAnalyzer] — on-device LiteRT-LM Gemma 4, offline-first
 *
 * Future analyzers to add here:
 *  • GeminiLiveAnalyzer — cloud WebSocket (add when API key is available)
 *  • RegexRuleAnalyzer  — fast deterministic rule set
 */
@Module
@InstallIn(SingletonComponent::class)
object SpamDetectionModule {

    /**
     * Ordered list of active analyzers consumed by [ThreatFusionEngine].
     * [GemmaAnalyzer] is provided by Dagger via its @Inject constructor and
     * delegates to the app's shared offline Gemma [TextGenerator].
     */
    @Provides
    @Singleton
    fun provideAnalyzers(
        gemmaAnalyzer: GemmaAnalyzer,
    ): List<@JvmSuppressWildcards ThreatAnalyzer> = listOf(gemmaAnalyzer)
}
