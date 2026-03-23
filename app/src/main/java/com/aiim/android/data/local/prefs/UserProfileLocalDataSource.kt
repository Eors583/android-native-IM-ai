package com.aiim.android.data.local.prefs

import android.content.Context
import com.aiim.android.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileLocalDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun load(): UserProfile {
        return UserProfile(
            avatarUri = prefs.getString(KEY_AVATAR_URI, "") ?: "",
            username = prefs.getString(KEY_USERNAME, prefs.getString(KEY_NAME, "")) ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            gender = prefs.getString(KEY_GENDER, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: ""
        )
    }

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_AVATAR_URI, profile.avatarUri)
            .putString(KEY_USERNAME, profile.username)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_GENDER, profile.gender)
            .putString(KEY_PHONE, profile.phone)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "user_profile_prefs"
        private const val KEY_AVATAR_URI = "key_avatar_uri"
        private const val KEY_USERNAME = "key_username"
        // 兼容旧版本字段
        private const val KEY_NAME = "key_name"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_GENDER = "key_gender"
        private const val KEY_PHONE = "key_phone"
    }
}
