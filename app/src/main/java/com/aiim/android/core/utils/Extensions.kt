package com.aiim.android.core.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform

/**
 * Kotlin扩展函数集合
 */

// Context扩展函数

/**
 * 显示Toast消息
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * 显示长Toast消息
 */
fun Context.showLongToast(message: String) {
    showToast(message, Toast.LENGTH_LONG)
}

// Compose扩展函数

/**
 * 在Compose中显示Toast
 */
@Composable
fun rememberToast(): (String) -> Unit {
    val context = LocalContext.current
    return { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

// Flow扩展函数

/**
 * 当Flow发射新值时执行副作用
 */
fun <T> Flow<T>.onEachValue(action: (T) -> Unit): Flow<T> {
    return this.onEach(action)
}

/**
 * 过滤空值
 */
fun <T> Flow<T?>.filterNotNull(): Flow<T> {
    return this.transform { value ->
        if (value != null) {
            emit(value)
        }
    }
}

// String扩展函数

/**
 * 检查字符串是否为有效的IP地址
 */
fun String.isValidIpAddress(): Boolean {
    return NetworkUtils.isValidIpAddress(this)
}

/**
 * 如果字符串为空则返回默认值
 */
fun String?.orDefault(default: String = ""): String {
    return this?.takeIf { it.isNotBlank() } ?: default
}

/**
 * 限制字符串长度
 */
fun String.limitLength(maxLength: Int, ellipsis: String = "..."): String {
    return if (this.length > maxLength) {
        this.substring(0, maxLength - ellipsis.length) + ellipsis
    } else {
        this
    }
}

// 集合扩展函数

/**
 * 批量转换列表
 */
fun <T, R> List<T>.mapIf(
    condition: (T) -> Boolean,
    transform: (T) -> R,
    elseTransform: (T) -> R
): List<R> {
    return this.map { item ->
        if (condition(item)) transform(item) else elseTransform(item)
    }
}

// 数字扩展函数

/**
 * 将Int转换为端口号字符串
 */
fun Int.toPortString(): String {
    return if (this in 1..65535) {
        this.toString()
    } else {
        "无效端口"
    }
}

// 布尔扩展函数

/**
 * 布尔值转换为连接状态文本
 */
fun Boolean.toConnectionStatus(): String {
    return if (this) "已连接" else "未连接"
}

/**
 * 布尔值转换为启用/禁用文本
 */
fun Boolean.toEnabledText(): String {
    return if (this) "启用" else "禁用"
}