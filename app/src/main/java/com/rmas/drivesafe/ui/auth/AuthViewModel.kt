package com.rmas.drivesafe.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.rmas.drivesafe.model.User
import com.rmas.drivesafe.repository.AuthRepository
import com.rmas.drivesafe.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                _authState.value = AuthState.Success(result.getOrNull()!!)
            } else {
                _authState.value = AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Greska pri prijavi"
                )
            }
        }
    }

    fun register(
        email: String,
        password: String,
        fullName: String,
        username: String,
        phone: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val authResult = authRepository.register(email, password)
            if (authResult.isSuccess) {
                val firebaseUser = authResult.getOrNull()!!
                val user = User(
                    id = firebaseUser.uid,
                    username = username,
                    fullName = fullName,
                    phone = phone,
                    profileImageUrl = "",
                    points = 0,
                    rank = "Pocetnik"
                )
                val userResult = userRepository.createUser(user)
                if (userResult.isSuccess) {
                    _authState.value = AuthState.Success(firebaseUser)
                } else {
                    _authState.value = AuthState.Error(
                        userResult.exceptionOrNull()?.message ?: "Greska pri cuvanju podataka"
                    )
                }
            } else {
                _authState.value = AuthState.Error(
                    authResult.exceptionOrNull()?.message ?: "Greska pri registraciji"
                )
            }
        }
    }
}