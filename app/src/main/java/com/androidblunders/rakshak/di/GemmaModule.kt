package com.androidblunders.rakshak.di

import com.androidblunders.rakshak.core.contract.TextGenerator
import com.androidblunders.rakshak.gemma.GemmaTextGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the offline Gemma 4 endpoint. Any consumer injects [TextGenerator]; only
 * this line decides the concrete engine, so a mock or a different on-device model
 * can be swapped in without touching a single caller.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GemmaModule {

    @Binds
    @Singleton
    abstract fun bindTextGenerator(impl: GemmaTextGenerator): TextGenerator
}
