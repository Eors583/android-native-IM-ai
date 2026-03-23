package com.aiim.android.domain.model

/**
 * 本地个人信息
 */
data class UserProfile(
    val avatarUri: String = "",
    val username: String = "",
    val email: String = "",
    val gender: String = "",
    val phone: String = ""
)
