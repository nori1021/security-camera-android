package com.securitycamera.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securitycamera.app.data.AppSettings
import com.securitycamera.app.data.PrefsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefsRepository: PrefsRepository,
    onBack: () -> Unit,
    onOpenUserEnrollment: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val settings by prefsRepository.settings.collectAsState(initial = AppSettings())

    var notifyEmail by remember(settings.notifyEmail) { mutableStateOf(settings.notifyEmail) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Enter the email address for alerts when an unknown face is detected.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Text("Notification email", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = notifyEmail,
                onValueChange = { notifyEmail = it },
                label = { Text("Alert recipient (unknown face)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        prefsRepository.updateNotifyEmail(notifyEmail)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save email")
            }

            Spacer(Modifier.height(32.dp))
            Text("Face enrollment", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose a person from the user list and capture photos to register.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenUserEnrollment,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Face & user enrollment")
            }
        }
    }
}
