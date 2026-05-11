package com.securitycamera.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onUsers: () -> Unit,
    onMonitor: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SecurityCamera", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Enroll faces, monitor, and alert email",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(onClick = onMonitor, modifier = Modifier.padding(8.dp)) { Text("Start monitoring") }
        Button(onClick = onUsers, modifier = Modifier.padding(8.dp)) { Text("Face & users") }
        Button(onClick = onSettings, modifier = Modifier.padding(8.dp)) { Text("Alert email") }
    }
}
