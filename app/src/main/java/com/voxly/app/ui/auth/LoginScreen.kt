package com.voxly.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean) -> Unit,
    viewModel: com.voxly.app.ui.viewmodel.LoginViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    // Observe login success
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess(uiState.isExistingUser)
        }
    }

    // Phone Number Hint API
    val hintLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val credential = result.data?.getParcelableExtra<com.google.android.gms.auth.api.credentials.Credential>(com.google.android.gms.auth.api.credentials.Credential.EXTRA_KEY)
            if (credential != null) {
                // If ID is a phone number (e.g. +91...), update it
                viewModel.updatePhoneNumber(credential.id)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.phoneNumber.isEmpty()) {
            try {
                val hintRequest = com.google.android.gms.auth.api.credentials.HintRequest.Builder()
                    .setPhoneNumberIdentifierSupported(true)
                    .build()
                val credentialsClient = com.google.android.gms.auth.api.credentials.Credentials.getClient(context)
                val intent = credentialsClient.getHintPickerIntent(hintRequest)
                hintLauncher.launch(
                    androidx.activity.result.IntentSenderRequest.Builder(intent.intentSender).build()
                )
            } catch (e: Exception) {
                // Hint failed or canceled, user can type manually
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Voxly",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Error Message
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        }

        if (!uiState.isCodeSent) {
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = { 
                    // Allow up to 15 chars for international numbers
                    if (it.length <= 15) viewModel.updatePhoneNumber(it) 
                },
                label = { Text("Phone Number") },
                prefix = { 
                     // Only show prefix if user hasn't typed '+' (which hints usually include)
                     if (!uiState.phoneNumber.startsWith("+")) Text("+91 ") 
                },
                placeholder = { Text("00000 00000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { activity?.let { viewModel.sendVerificationCode(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.phoneNumber.isNotEmpty() && !uiState.isLoading
            ) {
                Text("Get OTP")
            }
        } else {
            OutlinedTextField(
                value = uiState.otp,
                onValueChange = { viewModel.updateOtp(it) },
                label = { Text("Enter OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.verifyCode() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.otp.isNotEmpty() && !uiState.isLoading
            ) {
                Text("Verify & Continue")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
