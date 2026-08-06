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
                    text = "Versi ${BuildConfig.VERSION_NAME}",
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
                    onConfirm = { authViewModel.changePassword(it) }
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
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Text(
        text = "Ganti Password",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Password Anda masih default. Harap ganti untuk keamanan.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    if (error != null) {
        Text(text = error, color = MaterialTheme.colorScheme.error)
    }

    OutlinedTextField(
        value = newPassword,
        onValueChange = { newPassword = it },
        label = { Text("Password Baru") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = { Text("Konfirmasi Password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        Button(
            onClick = {
                if (newPassword == confirmPassword && newPassword.length >= 6) {
                    onConfirm(newPassword)
                }
            },
            enabled = newPassword == confirmPassword && newPassword.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan Password")
        }
    }
}
