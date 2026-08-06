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
    
    if (isInitialLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backStack = rememberNavBackStack(
        if (user != null) Route.Home else Route.Login
    )

    LaunchedEffect(user) {
        backStack.clear()
        backStack.add(if (user != null) Route.Home else Route.Login)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { 
            if (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
        }
    ) { route ->
        val r = route as Route
        when (r) {
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
                // Handle other routes if they are somehow reached from top level
                // or redirect to Home
                Text("Redirecting...")
                LaunchedEffect(Unit) {
                    backStack.clear()
                    backStack.add(Route.Home)
                }
            }
        }
    }
}