package com.aiim.android.core.im

import android.util.Log
import com.aiim.android.core.utils.Constants
import com.aiim.android.data.remote.model.SocketMessage
import com.aiim.android.domain.model.ConnectionState
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * 局域网 Socket：一台设备「启动服务器」监听端口，另一台「连接服务器」输入对方 IP。
 * 建链后双方共用 [peerSocket] 收发 JSON 行（每消息一行）。
 */
class SocketManager {

    private val gson = Gson()
    private val ioScope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedMessages = MutableStateFlow<SocketMessage?>(null)
    val receivedMessages: StateFlow<SocketMessage?> = _receivedMessages.asStateFlow()

    /** 当前与对端通信的套接字（客户端连上后的 socket，或服务端 accept 得到的 socket） */
    private var peerSocket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null

    private var currentNickname = Constants.DEFAULT_NICKNAME

    fun startServer(port: Int = Constants.SOCKET_PORT) {
        if (serverJob?.isActive == true || serverSocket?.isClosed == false) {
            Log.w(TAG, "服务器任务已在运行")
            return
        }
        if (isBusy()) {
            Log.w(TAG, "已有连接或正在连接，请先断开")
            return
        }

        serverJob = ioScope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    soTimeout = 0
                    bind(InetSocketAddress("0.0.0.0", port))
                }
                Log.d(TAG, "正在监听端口 $port，等待对方连接…")

                val client = serverSocket?.accept() ?: return@launch
                Log.d(TAG, "对端已连接: ${client.inetAddress.hostAddress}")
                attachPeer(client)
            } catch (e: SocketException) {
                if (serverSocket?.isClosed != true) {
                    Log.e(TAG, "服务器 Socket 异常", e)
                    _connectionState.value = ConnectionState.Failed(e.message ?: "服务器已关闭")
                }
            } catch (e: Exception) {
                Log.e(TAG, "启动服务器失败", e)
                _connectionState.value = ConnectionState.Failed(e.message ?: "未知错误")
            } finally {
                cleanupServerSocketOnly()
            }
        }
    }

    fun connectToServer(serverIp: String, port: Int = Constants.SOCKET_PORT) {
        val host = serverIp.trim()
        if (host.isEmpty()) {
            _connectionState.value = ConnectionState.Failed("IP 地址为空")
            return
        }
        if (isBusy()) {
            Log.w(TAG, "已有连接或正在连接")
            return
        }

        clientJob = ioScope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                Log.d(TAG, "正在连接: $host:$port")

                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), Constants.SOCKET_TIMEOUT)
                attachPeer(socket)
                Log.d(TAG, "已连接到服务器")
            } catch (e: Exception) {
                Log.e(TAG, "连接失败", e)
                _connectionState.value = ConnectionState.Failed(e.message ?: "连接失败")
                closePeerQuietly()
            }
        }
    }

    fun sendMessage(message: SocketMessage) {
        ioScope.launch {
            val socket = peerSocket
            if (socket == null || socket.isClosed || !socket.isConnected) {
                Log.e(TAG, "未与对端建立连接，无法发送")
                return@launch
            }
            try {
                val output = PrintWriter(socket.getOutputStream(), true)
                output.println(gson.toJson(message))
                Log.d(TAG, "已发送: ${message.content}")
            } catch (e: Exception) {
                Log.e(TAG, "发送失败", e)
            }
        }
    }

    fun disconnect() {
        ioScope.launch {
            // 先关 ServerSocket，才能从 accept() 里唤醒出来
            try {
                serverSocket?.close()
            } catch (_: Exception) {
            }
            serverSocket = null

            heartbeatJob?.cancel()
            receiveJob?.cancel()
            serverJob?.cancel()
            clientJob?.cancel()
            heartbeatJob = null
            receiveJob = null
            serverJob = null
            clientJob = null

            closePeerQuietly()

            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "已断开")
        }
    }

    fun setNickname(nickname: String) {
        currentNickname = nickname
    }

    fun getCurrentState(): ConnectionState = _connectionState.value

    private fun isBusy(): Boolean {
        return when (_connectionState.value) {
            is ConnectionState.Connected, is ConnectionState.Connecting -> true
            else -> false
        }
    }

    private fun attachPeer(socket: Socket) {
        closePeerQuietly()
        peerSocket = socket
        _connectionState.value = ConnectionState.Connected
        startReceivingMessages()
        startHeartbeat()
    }

    private fun closePeerQuietly() {
        try {
            peerSocket?.close()
        } catch (_: Exception) {
        }
        peerSocket = null
    }

    private fun cleanupServerSocketOnly() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    private fun startReceivingMessages() {
        receiveJob?.cancel()
        receiveJob = ioScope.launch {
            val socket = peerSocket ?: return@launch
            try {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (true) {
                    val line = input.readLine() ?: break
                    processIncomingMessage(line)
                }
            } catch (e: Exception) {
                Log.e(TAG, "接收结束或异常", e)
            } finally {
                Log.d(TAG, "对端可能已断开")
                _connectionState.value = ConnectionState.Disconnected
                closePeerQuietly()
                cleanupServerSocketOnly()
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = ioScope.launch {
            while (true) {
                val socket = peerSocket
                if (socket == null || socket.isClosed || !socket.isConnected) break
                try {
                    delay(Constants.HEARTBEAT_INTERVAL)
                    sendMessage(SocketMessage.createHeartbeat(currentNickname))
                } catch (e: Exception) {
                    Log.e(TAG, "心跳失败", e)
                    break
                }
            }
        }
    }

    private fun processIncomingMessage(jsonMessage: String) {
        try {
            val message = gson.fromJson(jsonMessage, SocketMessage::class.java)
            if (message.messageType == "heartbeat") {
                Log.d(TAG, "收到心跳")
                return
            }
            Log.d(TAG, "收到消息: ${message.content} from ${message.sender}")
            _receivedMessages.value = message
        } catch (e: Exception) {
            Log.e(TAG, "解析失败: $jsonMessage", e)
        }
    }

    companion object {
        private const val TAG = "SocketManager"
    }
}
