package com.aiim.android.domain.usecase

import com.aiim.android.domain.repository.ChatRepository
import javax.inject.Inject

class SetNicknameUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(nickname: String) {
        repository.setNickname(nickname)
    }
}
