package com.aiim.android.core.utils

/**
 * 应用常量定义
 */
object Constants {

    // Socket相关常量
    const val SOCKET_PORT = 8080
    const val SOCKET_TIMEOUT = 15_000 // 15 秒连接超时（部分路由器/热点较慢）
    const val HEARTBEAT_INTERVAL = 30000L // 30秒心跳间隔
    const val RECONNECT_DELAY = 3000L // 3秒重连延迟
    const val BUFFER_SIZE = 1024 * 8 // 8KB缓冲区

    // 数据库相关常量
    const val DATABASE_NAME = "aiim_chat.db"
    const val DATABASE_VERSION = 2

    // 消息类型
    const val MESSAGE_TYPE_TEXT = "text"
    const val MESSAGE_TYPE_HEARTBEAT = "heartbeat"
    const val MESSAGE_TYPE_SYSTEM = "system"

    // 消息状态
    const val MESSAGE_STATUS_SENDING = "sending"
    const val MESSAGE_STATUS_SENT = "sent"
    const val MESSAGE_STATUS_DELIVERED = "delivered"
    const val MESSAGE_STATUS_FAILED = "failed"

    // 连接状态
    const val CONNECTION_STATE_DISCONNECTED = "disconnected"
    const val CONNECTION_STATE_CONNECTING = "connecting"
    const val CONNECTION_STATE_CONNECTED = "connected"
    const val CONNECTION_STATE_FAILED = "failed"

    // 默认值
    const val DEFAULT_NICKNAME = "匿名用户"
    const val DEFAULT_SERVER_IP = "192.168.1.100" // 默认服务器IP

    // SharedPreferences键名
    const val PREF_NICKNAME = "pref_nickname"
    const val PREF_SERVER_IP = "pref_server_ip"
    const val PREF_LAST_CONNECTED_IP = "pref_last_connected_ip"

    // Intent extra键名
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_SERVER_IP = "extra_server_ip"
    const val EXTRA_CONNECTION_STATE = "extra_connection_state"
}