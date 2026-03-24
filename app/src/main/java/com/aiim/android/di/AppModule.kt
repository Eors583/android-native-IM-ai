package com.aiim.android.di

import android.content.Context
import com.aiim.android.data.ai.MnnOnDeviceQaEngine
import com.aiim.android.data.local.database.ChatDatabase
import com.aiim.android.data.repository.ChatRepositoryImpl
import com.aiim.android.domain.ai.OnDeviceQaEngine
import com.aiim.android.domain.repository.ChatRepository
import com.aiim.android.core.im.SocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt依赖注入模块
 * 提供应用全局单例依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供Room数据库实例
     */
    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {
        return ChatDatabase.getInstance(context)
    }

    /**
     * 提供Socket管理器实例
     */
    @Provides
    @Singleton
    fun provideSocketManager(): SocketManager {
        return SocketManager()
    }

    /**
     * 提供聊天仓库实现
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        database: ChatDatabase,
        socketManager: SocketManager
    ): ChatRepository {
        return ChatRepositoryImpl(
            messageDao = database.messageDao(),
            chatRoomDao = database.chatRoomDao(),
            socketManager = socketManager
        )
    }

    @Provides
    @Singleton
    fun provideOnDeviceQaEngine(
        engine: MnnOnDeviceQaEngine
    ): OnDeviceQaEngine {
        return engine
    }
}