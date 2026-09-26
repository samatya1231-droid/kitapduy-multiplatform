package com.semtinazizi.kitapduy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.data.repository.StorageRepository
import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import com.semtinazizi.kitapduy.platform.BillingManager

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(authRepository.isUserLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.authenticationChanges.collectLatest { loggedIn ->
                if (loggedIn != null) {
                    _isLoggedIn.value = loggedIn
                    if (loggedIn) authRepository.currentUserUid?.let(::observeUserProfile)
                    else {
                        profileJob?.cancel()
                        _userProfile.value = null
                    }
                }
            }
        }
    }

    suspend fun checkAuthStatus(): Boolean {
        authRepository.awaitSessionInitialization()
        val loggedIn = authRepository.isUserLoggedIn
        _isLoggedIn.value = loggedIn
        
        if (loggedIn) {
            val uid = authRepository.currentUserUid
            if (uid != null) {
                observeUserProfile(uid)
            }
        }
        return loggedIn
    }

    private fun observeUserProfile(uid: String) {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            combine(authRepository.getUserProfileFlow(uid), billingManager.isPremium) { profile, storePremium ->
                profile?.copy(isPremium = profile.hasPremium() || storePremium)
            }.collectLatest { profile ->
                _userProfile.value = profile
            }
        }
    }

    fun updateProfilePicture(bytes: ByteArray, extension: String, onResult: (Boolean) -> Unit) {
        val uid = authRepository.currentUserUid
        if (uid == null) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            val uploadResult = storageRepository.uploadProfilePicture(uid, bytes, extension)
            if (uploadResult.isSuccess) {
                val publicUrl = uploadResult.getOrNull()
                if (publicUrl != null) {
                    val dbResult = authRepository.updateProfilePhoto(uid, publicUrl)
                    onResult(dbResult)
                    if (dbResult) {
                        observeUserProfile(uid) // Refresh
                    }
                } else {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        }
    }

    fun removeProfilePicture(onResult: (Boolean) -> Unit) {
        val uid = authRepository.currentUserUid
        if (uid == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val dbResult = authRepository.updateProfilePhoto(uid, null)
            onResult(dbResult)
            if (dbResult) {
                observeUserProfile(uid)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _error.value = null
            try {
                authRepository.signOut()
                _isLoggedIn.value = false
                _userProfile.value = null
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _error.value = "Çıkış yapılamadı. Lütfen tekrar deneyin." }
        }
    }

    suspend fun signIn(email: String, pass: String): Boolean {
        val success = authRepository.signInWithEmail(email, pass)
        if (success) {
            _isLoggedIn.value = true
            authRepository.currentUserUid?.let { observeUserProfile(it) }
        }
        return success
    }

    suspend fun signUp(email: String, pass: String, name: String): Boolean {
        val success = authRepository.signUpWithEmail(email, pass, name)
        if (success) {
            _isLoggedIn.value = authRepository.isUserLoggedIn
            if (!authRepository.isUserLoggedIn) {
                _error.value = "Kaydınız alındı. E-posta adresinize gelen doğrulama bağlantısını açıp giriş yapın."
            }
            authRepository.currentUserUid?.let { observeUserProfile(it) }
        }
        return success
    }

    
    suspend fun signInAnonymously(name: String): Boolean {
        _error.value = null
        val result = authRepository.signInAnonymously(name)
        if (result) {
            _isLoggedIn.value = true
            authRepository.currentUserUid?.let(::observeUserProfile)
        } else {
            _error.value = "Misafir girişi tamamlanamadı. Bağlantınızı kontrol edip tekrar deneyin veya e-posta ile giriş yapın."
        }
        return result
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun signInWithGoogle(): Boolean {
        val success = authRepository.signInWithGoogle()
        if (success) {
            _isLoggedIn.value = true
            authRepository.currentUserUid?.let { observeUserProfile(it) }
        }
        return success
    }

    suspend fun signInWithApple(): Boolean {
        val success = authRepository.signInWithApple()
        if (success) {
            _isLoggedIn.value = true
            authRepository.currentUserUid?.let { observeUserProfile(it) }
        }
        return success
    }

    suspend fun getGoogleOAuthUrl(): String {
        return authRepository.getGoogleOAuthUrl()
    }

    suspend fun getAppleOAuthUrl(): String {
        return authRepository.getAppleOAuthUrl()
    }

    suspend fun updateProfile(name: String, bio: String, username: String?): Boolean {
        val uid = authRepository.currentUserUid ?: return false
        val success = authRepository.updateProfile(uid, name, bio, username)
        if (success) {
            // Profil başarılı bir şekilde güncellendi, lokal akışı tetikleyelim veya yenileyelim
            val updatedProfile = authRepository.getUserProfile(uid)
            _userProfile.value = updatedProfile?.copy(isPremium = updatedProfile.hasPremium() || billingManager.isPremium.value)
        }
        return success
    }
}
