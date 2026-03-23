package com.aiim.android.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aiim.android.core.utils.Constants
import com.aiim.android.data.local.dao.ChatRoomDao
import com.aiim.android.data.local.dao.MessageDao
import com.aiim.android.data.local.entity.MessageEntity
import com.aiim.android.data.local.entity.ChatRoomEntity
import com.aiim.android.data.local.converters.DateConverter

/**
 * Room数据库类
 * 管理聊天消息的数据库实例
 */
@Database(
    entities = [MessageEntity::class, ChatRoomEntity::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class ChatDatabase : RoomDatabase() {

    /**
     * 获取消息数据访问对象
     */
    abstract fun messageDao(): MessageDao

    abstract fun chatRoomDao(): ChatRoomDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        /**
         * 获取数据库单例实例
         */
        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // 数据库版本升级时，简化处理：清空重建
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}