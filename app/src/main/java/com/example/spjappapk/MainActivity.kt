package com.example.spjappapk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.NavEntry
import com.example.spjappapk.ui.auth.AuthViewModel
import com.example.spjappapk.ui.auth.LoginScreen
import com.example.spjappapk.ui.home.HomeScreen
import com.example.spjappapk.ui.navigation.Route
import com.example.spjappapk.ui.theme.SPJAPPAPKTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SPJAPPAPKTheme {
                val app = application as SPJApplication
                val authViewModel: AuthViewModel = viewModel {
                    AuthViewModel(app.sessionManager)
                }
                MainApp(authViewModel)
            }
        }
    }
}

@Composable
fun MainApp(authViewModel: AuthViewModel) {
    val user by authViewModel.user.collectAsState()
    val isInitialLoading by authViewModel.isInitialLoading.collectAsState()
    val needsPasswordChange by authViewModel.needsPasswordChange.collectAsState()

    if (isInitialLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Tentukan route awal berdasarkan status login
    // Jika perlu ganti password, tetap di Login tapi dengan form ganti password
    val targetRoute = if (user != null && !needsPasswordChange) Route.Home else Route.Login
    
    val backStack = rememberNavBackStack(targetRoute)

    LaunchedEffect(user, needsPasswordChange) {
        val current = backStack.lastOrNull()
        if (current != targetRoute) {
            // Update backstack safely: add new route, then remove old one if needed
            // to ensure it is never empty during the process.
            if (backStack.isEmpty()) {
                backStack.add(targetRoute)
            } else {
                backStack.add(targetRoute)
                while (backStack.size > 1) {
                    backStack.removeAt(0)
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { 
            if (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
        }
    ) { route ->
        when (val r = route as Route) {
            is Route.Login -> NavEntry(r) {
                LoginScreen(authViewModel = authViewModel)
            }
            is Route.Home -> NavEntry(r) {
                HomeScreen(
                    currentUser = user,
                    onLogout = {
                        authViewModel.logout()
                    }
                )
            }
            else -> NavEntry(r) {
                // Redirect ke Dashboard jika route tidak dikenal di level ini
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                LaunchedEffect(Unit) {
                    if (backStack.lastOrNull() != targetRoute) {
                        backStack.clear()
                        backStack.add(targetRoute)
                    }
                }
            }
        }
    }
}