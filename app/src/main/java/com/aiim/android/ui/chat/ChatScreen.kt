package com.aiim.android.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.aiim.android.core.utils.NetworkUtils
import com.aiim.android.ui.chat.chatroom.ChatRoomScreen
import com.aiim.android.ui.chat.connection.ConnectionScreen
import com.aiim.android.ui.chat.history.ChatRoomsScreen
import com.aiim.android.ui.chat.layout.rememberChatLayoutSpec
import com.aiim.android.ui.ai.AiChatScreen
import com.aiim.android.ui.profile.ProfileScreen
import com.aiim.android.ui.profile.ProfileViewModel

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
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileUiState by profileViewModel.uiState.collectAsState()
    var bottomTab by rememberSaveable { mutableStateOf(BottomTab.Home) }
    val shouldShowBottomBar = mainScreen != MainScreen.ChatRoom

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearErrorMessage()
        }
    }
    LaunchedEffect(profileUiState.username) {
        viewModel.syncNicknameFromProfile(profileUiState.username)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = bottomTab == BottomTab.Home,
                        onClick = { bottomTab = BottomTab.Home },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "首页") },
                        label = { Text("首页") }
                    )
                    NavigationBarItem(
                        selected = bottomTab == BottomTab.ChatRooms,
                        onClick = { bottomTab = BottomTab.ChatRooms },
                        icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = "聊天室") },
                        label = { Text("聊天室") }
                    )
                    NavigationBarItem(
                        selected = bottomTab == BottomTab.AI,
                        onClick = { bottomTab = BottomTab.AI },
                        icon = { Icon(imageVector = Icons.Default.SmartToy, contentDescription = "AI聊天") },
                        label = { Text("AI聊天") }
                    )
                    NavigationBarItem(
                        selected = bottomTab == BottomTab.Profile,
                        onClick = { bottomTab = BottomTab.Profile },
                        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "我的") },
                        label = { Text("我的") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (mainScreen == MainScreen.ChatRoom) {
                ChatRoomScreen(
                    uiState = uiState,
                    inputState = inputState,
                    viewModel = viewModel,
                    layoutSpec = layoutSpec,
                )
            } else {
                when (bottomTab) {
                    BottomTab.Home -> ConnectionScreen(
                        uiState = uiState,
                        inputState = inputState,
                        networkTypeLabel = networkTypeLabel,
                        viewModel = viewModel,
                        layoutSpec = layoutSpec,
                    )
                    BottomTab.ChatRooms -> ChatRoomsScreen(
                        rooms = uiState.chatRooms,
                        onOpenRoom = viewModel::openHistoryChatRoom
                    )
                    BottomTab.AI -> AiChatScreen()
                    BottomTab.Profile -> ProfileScreen(
                        viewModel = profileViewModel,
                        showMessage = { message -> snackbarHostState.showSnackbar(message) }
                    )
                }
            }
        }
    }
}

private enum class BottomTab {
    Home,
    ChatRooms,
    AI,
    Profile
}
