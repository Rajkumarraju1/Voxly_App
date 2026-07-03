package com.voxly.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxly.app.data.model.User
import com.voxly.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val language: String = "",
    val gender: String = "",
    val avatarUrl: String = "",
    val displayName: String = "",
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateLanguage(language: String) {
        _uiState.update { it.copy(language = language) }
    }

    fun updateGender(gender: String) {
        _uiState.update { it.copy(gender = gender) }
    }

    fun updateAvatar(avatarUrl: String) {
        _uiState.update { it.copy(avatarUrl = avatarUrl) }
    }

    fun updateProfile(displayName: String, tags: List<String>) {
        _uiState.update { it.copy(displayName = displayName, tags = tags) }
    }

    fun saveUser(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentState = _uiState.value
                val user = User(
                    languages = listOf(currentState.language),
                    gender = currentState.gender,
                    avatarUrl = currentState.avatarUrl,
                    displayName = currentState.displayName,
                    tags = currentState.tags,
                    coins = 0.0 // Initial bonus
                )
                userRepository.saveUser(user)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
