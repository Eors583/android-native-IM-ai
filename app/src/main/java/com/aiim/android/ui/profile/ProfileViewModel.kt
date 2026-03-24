package com.aiim.android.ui.profile

import androidx.lifecycle.ViewModel
import com.aiim.android.data.local.prefs.UserProfileLocalDataSource
import com.aiim.android.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileLocalDataSource: UserProfileLocalDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val emailRegex =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    init {
        val profile = userProfileLocalDataSource.load()
        _uiState.value = ProfileUiState.fromProfile(profile)
    }

    fun updateAvatarUri(uri: String) {
        _uiState.update { it.copy(avatarUri = uri) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updateEmail(value: String) {
        val v = value
        _uiState.update {
            it.copy(
                email = v,
                emailError = validateEmail(v),
            )
        }
    }

    fun updateGender(value: String) {
        _uiState.update { it.copy(gender = value) }
    }

    fun updatePhone(value: String) {
        val v = value
        _uiState.update {
            it.copy(
                phone = v,
                phoneError = validatePhone(v),
            )
        }
    }

    fun saveProfile(): Boolean {
        val email = _uiState.value.email.trim()
        val phone = _uiState.value.phone.trim()
        val emailErr = validateEmail(email)
        val phoneErr = validatePhone(phone)

        if (emailErr != null || phoneErr != null) {
            _uiState.update {
                it.copy(
                    emailError = emailErr,
                    phoneError = phoneErr,
                    lastSaveMessage = null
                )
            }
            return false
        }

        val profile = UserProfile(
            avatarUri = _uiState.value.avatarUri.trim(),
            username = _uiState.value.username.trim(),
            email = email,
            gender = _uiState.value.gender.trim(),
            phone = phone
        )
        userProfileLocalDataSource.save(profile)
        _uiState.update {
            it.copy(
                emailError = null,
                phoneError = null,
                lastSaveMessage = "个人信息已保存到本地"
            )
        }
        return true
    }

    fun consumeSaveMessage() {
        _uiState.update { it.copy(lastSaveMessage = null) }
    }
}

data class ProfileUiState(
    val avatarUri: String = "",
    val username: String = "",
    val email: String = "",
    val emailError: String? = null,
    val gender: String = "",
    val phone: String = "",
    val phoneError: String? = null,
    val lastSaveMessage: String? = null
) {
    companion object {
        fun fromProfile(profile: UserProfile): ProfileUiState {
            return ProfileUiState(
                avatarUri = profile.avatarUri,
                username = profile.username,
                email = profile.email,
                emailError = null,
                gender = profile.gender,
                phone = profile.phone,
                phoneError = null
            )
        }
    }
}

private fun ProfileViewModel.validateEmail(email: String): String? {
    if (email.isBlank()) return null
    return if (emailRegex.matches(email)) null else "邮箱格式不正确"
}

private fun ProfileViewModel.validatePhone(phone: String): String? {
    if (phone.isBlank()) return null
    val digits = phone.filter { it.isDigit() }
    if (digits.length !in 7..15) return "电话格式不正确"
    return null
}
