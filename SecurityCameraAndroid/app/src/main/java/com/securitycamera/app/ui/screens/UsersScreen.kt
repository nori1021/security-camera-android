package com.securitycamera.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.securitycamera.app.data.AppSettings
import com.securitycamera.app.data.FunctionsApi
import com.securitycamera.app.data.PrefsRepository
import com.securitycamera.app.ui.navigation.RegistrationNavKeys
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    prefsRepository: PrefsRepository,
    onBack: () -> Unit,
    onEnroll: (String) -> Unit,
    savedStateHandle: SavedStateHandle,
) {
    val scope = rememberCoroutineScope()
    val settings by prefsRepository.settings.collectAsState(initial = AppSettings())
    val subjects = settings.enrolledSubjects.sorted().toList()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(savedStateHandle) {
        snapshotFlow {
            savedStateHandle.get<String>(RegistrationNavKeys.SUCCESS_SUBJECT_ID)
        }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { id ->
                snackbarHostState.showSnackbar("Registration complete: $id")
                savedStateHandle.remove<String>(RegistrationNavKeys.SUCCESS_SUBJECT_ID)
            }
    }

    var showAdd by remember { mutableStateOf(false) }
    var newId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Enrolled users") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                error = null
                newId = ""
                showAdd = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        if (subjects.isEmpty()) {
            Box(
                modifier = contentModifier.padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No enrolled users yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = contentModifier) {
                items(subjects, key = { it }) { id ->
                    ListItem(
                        headlineContent = { Text(id) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TextButton(onClick = { onEnroll(id) }) { Text("Retake") }
                                IconButton(onClick = {
                                    scope.launch { prefsRepository.removeEnrolledSubject(id) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("User ID") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it; error = null },
                        label = { Text("subjectId (letters, digits, -, _)") },
                        singleLine = true,
                    )
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = newId.trim()
                    if (!FunctionsApi.validateSubjectId(t)) {
                        error = "Use 1–128 characters: letters, digits, hyphen, underscore only."
                        return@TextButton
                    }
                    showAdd = false
                    onEnroll(t)
                }) { Text("Continue to capture") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            },
        )
    }
}
