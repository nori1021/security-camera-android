package com.securitycamera.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.securitycamera.app.ui.navigation.SecurityCameraNavHost
import com.securitycamera.app.ui.theme.SecurityCameraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SecurityCameraApplication
        setContent {
            SecurityCameraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    SecurityCameraNavHost(
                        navController = navController,
                        prefsRepository = app.prefsRepository,
                    )
                }
            }
        }
    }
}
