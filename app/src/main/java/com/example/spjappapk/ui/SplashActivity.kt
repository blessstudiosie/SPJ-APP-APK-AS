package com.example.spjappapk.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.spjappapk.MainActivity
import com.example.spjappapk.R
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                LaunchedEffect(Unit) {
                    delay(2000) // Tampilkan logo selama 2 detik
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }

                Image(
                    painter = painterResource(id = R.drawable.logo_spj),
                    contentDescription = "SPJ Logo",
                    modifier = Modifier.size(200.dp)
                )
            }
        }
    }
}
