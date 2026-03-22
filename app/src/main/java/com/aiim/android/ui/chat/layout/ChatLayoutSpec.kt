package com.aiim.android.ui.chat.layout

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 随 [WindowSizeClass] 变化的聊天相关布局参数，便于手机 / 平板 / 折叠屏统一约束内容最大宽度。
 */
@Immutable
data class ChatLayoutSpec(
    val screenPaddingHorizontal: Dp,
    val screenPaddingVertical: Dp,
    /** null 表示占满屏宽（手机竖屏）。 */
    val maxContentWidth: Dp?,
    val messageBubbleMaxFraction: Float,
) {
    companion object {
        val Compact = ChatLayoutSpec(
            screenPaddingHorizontal = 20.dp,
            screenPaddingVertical = 16.dp,
            maxContentWidth = null,
            messageBubbleMaxFraction = 0.82f,
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberChatLayoutSpec(windowSizeClass: WindowSizeClass): ChatLayoutSpec {
    val width = windowSizeClass.widthSizeClass
    return remember(width) {
        when (width) {
            WindowWidthSizeClass.Compact -> ChatLayoutSpec.Compact
            WindowWidthSizeClass.Medium -> ChatLayoutSpec(
                screenPaddingHorizontal = 28.dp,
                screenPaddingVertical = 20.dp,
                maxContentWidth = 680.dp,
                messageBubbleMaxFraction = 0.72f,
            )
            WindowWidthSizeClass.Expanded -> ChatLayoutSpec(
                screenPaddingHorizontal = 40.dp,
                screenPaddingVertical = 24.dp,
                maxContentWidth = 840.dp,
                messageBubbleMaxFraction = 0.62f,
            )
            else -> ChatLayoutSpec.Compact
        }
    }
}
