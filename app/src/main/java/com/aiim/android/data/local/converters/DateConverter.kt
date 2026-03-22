package com.aiim.android.data.local.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room数据库类型转换器
 * 用于在Room数据库中存储和读取Date类型
 */
class DateConverter {

    /**
     * 将Long时间戳转换为Date对象
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * 将Date对象转换为Long时间戳
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}