package com.androidblunders.rakshak.di

import com.androidblunders.rakshak.core.contract.MessageSource
import com.androidblunders.rakshak.core.contract.SpeechToTextEngine
import com.androidblunders.rakshak.core.contract.TextToSpeechEngine
import com.androidblunders.rakshak.core.contract.ThreatAnalyzer
import com.androidblunders.rakshak.core.contract.ThreatResponder
import com.androidblunders.rakshak.detection.GemmaThreatAnalyzer
import com.androidblunders.rakshak.detection.NotificationMessageSource
import com.androidblunders.rakshak.orchestrator.DefaultThreatFusionEngine
import com.androidblunders.rakshak.orchestrator.ThreatFusionEngine
import com.androidblunders.rakshak.stub.LoggingTextToSpeech
import com.androidblunders.rakshak.stub.LoggingThreatResponder
import com.androidblunders.rakshak.stub.NoOpSpeechToText
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * The single place the app is assembled. Every module plugs in here:
 *
 *  - `@Multibinds` declares the collections the orchestrator consumes (possibly empty).
 *  - `@IntoSet` adds a concrete analyzer / source / responder.
 *  - `@Binds` picks the active implementation of each single-slot contract.
 *
 * To add a real module later (e.g. GeminiLiveThreatAnalyzer, SmsReaderSource,
 * OverlayResponder): implement the interface and add ONE `@Binds @IntoSet` line.
 * The orchestrator, fusion engine and UI stay untouched.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OrchestratorModule {

    @Binds
    @Singleton
    abstract fun bindFusionEngine(impl: DefaultThreatFusionEngine): ThreatFusionEngine

    @Binds
    @Singleton
    abstract fun bindSpeechToText(impl: NoOpSpeechToText): SpeechToTextEngine

    @Binds
    @Singleton
    abstract fun bindTextToSpeech(impl: LoggingTextToSpeech): TextToSpeechEngine

    // --- Analyzers (fused) -------------------------------------------------
    @Multibinds
    abstract fun analyzers(): Set<ThreatAnalyzer>

    @Binds
    @IntoSet
    abstract fun bindGemmaAnalyzer(impl: GemmaThreatAnalyzer): ThreatAnalyzer

    // --- Message sources (fanned-in) --------------------------------------
    @Multibinds
    abstract fun messageSources(): Set<MessageSource>

    @Binds
    @IntoSet
    abstract fun bindNotificationSource(impl: NotificationMessageSource): MessageSource

    // --- Responders (intervention side-effects) ---------------------------
    @Multibinds
    abstract fun responders(): Set<ThreatResponder>

    @Binds
    @IntoSet
    abstract fun bindLoggingResponder(impl: LoggingThreatResponder): ThreatResponder
    
    @Binds
    @IntoSet
    abstract fun bindOverlayResponder(impl: com.androidblunders.rakshak.services.responder.OverlayThreatResponder): ThreatResponder

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
    }
}
