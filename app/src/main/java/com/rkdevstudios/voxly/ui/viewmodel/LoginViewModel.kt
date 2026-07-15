package com.rkdevstudios.voxly.ui.viewmodel


import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LoginUiState(
    val phoneNumber: String = "",
    val otp: String = "",
    val isCodeSent: Boolean = false,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val isExistingUser: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: com.rkdevstudios.voxly.data.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun updatePhoneNumber(number: String) {
        if (_uiState.value.isLoading || _uiState.value.isCodeSent) return
        var digits = number.filter { it.isDigit() }
        // If it starts with '91' and is longer than 10 digits (e.g. from picker +91...), strip '91' country code
        if (digits.startsWith("91") && digits.length > 10) {
            digits = digits.substring(2)
        }
        val truncated = if (digits.length > 10) digits.substring(0, 10) else digits
        _uiState.update { it.copy(phoneNumber = truncated, error = null) }
    }

    fun updateOtp(code: String) {
        _uiState.update { it.copy(otp = code, error = null) }
    }

    fun cancelVerification() {
        verificationId = null
        resendToken = null
        _uiState.update { it.copy(isLoading = false, isCodeSent = false, otp = "", error = null) }
    }

    fun sendVerificationCode(activity: Activity) {
        if (_uiState.value.isLoading) return
        val rawNumber = _uiState.value.phoneNumber
        if (rawNumber.length != 10) return

        val formattedNumber = "+91$rawNumber"

        _uiState.update { it.copy(isLoading = true, error = null) }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval or instant verification
                // Automatically verify code if detected
                val code = credential.smsCode
                if (code != null) {
                    _uiState.update { it.copy(otp = code) }
                    // Optional: Auto-submit
                    // signInWithPhoneAuthCredential(credential)
                } else {
                     signInWithPhoneAuthCredential(credential)
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Verification failed") }
            }

            override fun onCodeSent(
                vId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = vId
                resendToken = token
                _uiState.update { it.copy(isLoading = false, isCodeSent = true) }
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode() {
        val code = _uiState.value.otp
        val vid = verificationId
        if (code.isEmpty() || vid == null) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        val credential = PhoneAuthProvider.getCredential(vid, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
         viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Check if user exists in Firestore
                        val uid = task.result.user?.uid
                        if (uid != null) {
                            viewModelScope.launch {
                                val user = userRepository.getUser(uid)
                                val isExisting = user != null
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        loginSuccess = true,
                                        isExistingUser = isExisting
                                    )
                                }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loginSuccess = true,
                                    isExistingUser = false // If UID is null, treat as new user
                                )
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = task.exception?.localizedMessage ?: "Sign in failed"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}
