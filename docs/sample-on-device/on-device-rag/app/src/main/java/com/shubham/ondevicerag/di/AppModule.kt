package com.shubham.ondevicerag.di

import com.shubham.ondevicerag.feature.chat.data.repository.GemmaChatRepository
import com.shubham.ondevicerag.feature.chat.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindChatRepository(repository: GemmaChatRepository): ChatRepository

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
    }
}
