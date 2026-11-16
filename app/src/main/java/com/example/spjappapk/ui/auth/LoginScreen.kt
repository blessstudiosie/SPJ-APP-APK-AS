package com.example.spjappapk.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.spjappapk.BuildConfig
import com.example.spjappapk.R

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()
    val needsPasswordChange by authViewModel.needsPasswordChange.collectAsState()

    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TIME})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (needsPasswordChange) {
                ChangePasswordForm(
                    isLoading = isLoading,
                    error = error,
                    onConfirm = { authViewModel.changePassword(it) },
                    onCancel = { authViewModel.cancelPasswordChange() }
                )
            } else {
                LoginForm(
                    name = name,
                    onNameChange = { name = it },
                    password = password,
                    onPasswordChange = { password = it },
                    isLoading = isLoading,
                    error = error,
                    passwordFocusRequester = passwordFocusRequester,
                    onLogin = { authViewModel.login(name, password) }
                )
            }
        }
    }
}

@Composable
fun LoginForm(
    name: String,
    onNameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    passwordFocusRequester: FocusRequester,
    onLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Image(
        painter = painterResource(id = R.drawable.logo_spj),
        contentDescription = "Logo SPJ",
        modifier = Modifier
            .size(180.dp)
            .padding(bottom = 16.dp)
    )

    Text(
        text = "Sarana Prima Jaya", 
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Silakan Login untuk melanjutkan", 
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Nama Lengkap") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { passwordFocusRequester.requestFocus() }
        )
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(passwordFocusRequester),
        enabled = !isLoading,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onLogin()
            }
        )
    )
    Spacer(modifier = Modifier.height(24.dp))
    
    if (isLoading) {
        CircularProgressIndicator()
    } else {
        Button(
            onClick = { 
                focusManager.clearFocus()
                onLogin()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Sign In")
        }
    }
}

@Composable
fun ChangePasswordForm(
    isLoading: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit = {}
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    Text(
        text = "Wajib Ganti Password",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Password Anda masih default ('123'). Silakan buat password baru (minimal 4 karakter) sebelum masuk ke aplikasi.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    val displayError = localValidationError ?: error
    if (displayError != null) {
        Text(
            text = displayError,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    OutlinedTextField(
        value = newPassword,
        onValueChange = {
            newPassword = it
            localValidationError = null
        },
        label = { Text("Password Baru (Min. 4 Karakter)") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = {
            confirmPassword = it
            localValidationError = null
        },
        label = { Text("Konfirmasi Password Baru") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        Button(
            onClick = {
                val cleanNew = newPassword.trim()
                val cleanConfirm = confirmPassword.trim()
                when {
                    cleanNew.length < 4 -> localValidationError = "Password baru minimal terdiri dari 4 karakter."
                    cleanNew.equals("123", ignoreCase = true) || cleanNew.equals("ganti123", ignoreCase = true) ->
                        localValidationError = "Password baru tidak boleh menggunakan password bawaan ('123')."
                    cleanNew != cleanConfirm -> localValidationError = "Konfirmasi password baru tidak cocok."
                    else -> onConfirm(cleanNew)
                }
            },
            enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan Password & Masuk")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Batal / Kembali ke Login")
        }
    }
}
