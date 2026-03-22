package com.aiim.android.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aiim.android.core.utils.NetworkUtils
import com.aiim.android.ui.chat.chatroom.ChatRoomScreen
import com.aiim.android.ui.chat.connection.ConnectionScreen
import com.aiim.android.ui.chat.layout.rememberChatLayoutSpec

/**
 * 根界面：先连接服务器，成功后再进入聊天室。
 * 使用 [calculateWindowSizeClass] 约束大屏内容宽度，便于团队协作与多形态设备。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputState by viewModel.inputState.collectAsState()
    val mainScreen by viewModel.mainScreen.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val networkTypeLabel = NetworkUtils.getNetworkType(context)
    val activity = context as ComponentActivity
    val windowSizeClass = calculateWindowSizeClass(activity = activity)
    val layoutSpec = rememberChatLayoutSpec(windowSizeClass)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (mainScreen) {
                MainScreen.Connection -> ConnectionScreen(
                    uiState = uiState,
                    inputState = inputState,
                    networkTypeLabel = networkTypeLabel,
                    viewModel = viewModel,
                    layoutSpec = layoutSpec,
                )
                MainScreen.ChatRoom -> ChatRoomScreen(
                    uiState = uiState,
                    inputState = inputState,
                    viewModel = viewModel,
                    layoutSpec = layoutSpec,
                )
            }
        }
    }
}
