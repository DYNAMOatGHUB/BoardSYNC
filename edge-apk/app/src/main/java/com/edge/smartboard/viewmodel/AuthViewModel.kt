package com.edge.smartboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edge.smartboard.models.LoginResponse
import com.edge.smartboard.models.UserInfo
import com.edge.smartboard.repository.EdgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val response: LoginResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: EdgeRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isLoggedIn = repository.isLoggedIn()

    /** Saved server URL so the Login screen can pre-fill and update it. */
    val serverUrl: StateFlow<String> = repository.getServerUrlFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveServerUrl(url: String) {
        viewModelScope.launch { repository.saveServerUrl(url) }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, password)
            _authState.value = result.fold(
                onSuccess  = { AuthState.Success(it) },
                onFailure  = { AuthState.Error(it.message ?: "Unable to reach server. Use Demo Mode to explore the app.") }
            )
        }
    }

    /** Logs in offline using mock data — no backend required. */
    fun demoLogin() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val mockResponse = LoginResponse(
                access_token = "demo_token_offline",
                token_type   = "bearer",
                user = UserInfo(
                    id     = "demo-001",
                    name   = "Dr. Demo Teacher",
                    email  = "demo@edge.school",
                    school = "Edge Academy",
                    role   = "teacher"
                )
            )
            repository.saveToken(mockResponse.access_token)
            _authState.value = AuthState.Success(mockResponse)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
