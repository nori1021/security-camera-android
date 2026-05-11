package com.securitycamera.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.securitycamera.app.BuildConfig
import com.securitycamera.app.camera.awaitProcessCameraProvider
import com.securitycamera.app.camera.toJpegBytes
import com.securitycamera.app.data.AppSettings
import com.securitycamera.app.data.FunctionsApi
import com.securitycamera.app.data.PrefsRepository
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollScreen(
    subjectId: String,
    prefsRepository: PrefsRepository,
    onBack: () -> Unit,
    onRegistered: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settings by prefsRepository.settings.collectAsState(initial = AppSettings())
    val api = remember { FunctionsApi() }

    var camGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> camGranted = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    var shots by remember { mutableIntStateOf(0) }
    val jpegBuffers = remember { mutableListOf<ByteArray>() }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(lifecycleOwner, camGranted) {
        if (!camGranted) return@LaunchedEffect
        val provider = context.awaitProcessCameraProvider()
        provider.unbindAll()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registration: $subjectId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            if (!camGranted) {
                Text("Camera permission is required.", color = MaterialTheme.colorScheme.error)
                return@Column
            }
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Photos taken: $shots (one or more required to register)",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (busy) {
                Text(
                    "Registering... this can take a few minutes on cold start.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bytes = image.toJpegBytes()
                                image.close()
                                jpegBuffers.add(bytes)
                                shots = jpegBuffers.size
                                busy = false
                            }

                            override fun onError(exception: ImageCaptureException) {
                                message = exception.message.orEmpty()
                                busy = false
                            }
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Shutter") }

            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !busy && jpegBuffers.isNotEmpty(),
                onClick = {
                    if (settings.baseUrl.isBlank() || settings.functionKey.isBlank()) {
                        message =
                            if (BuildConfig.DEBUG) {
                                "Server not configured. Add functions.base.url and functions.key to local.properties, then rebuild."
                            } else {
                                "Cannot reach the server. Please try again later."
                            }
                        return@Button
                    }
                    busy = true
                    message = "Registering... this can take a few minutes on cold start."
                    scope.launch(Dispatchers.IO) {
                        val r = api.registerFace(
                            settings.baseUrl,
                            settings.functionKey,
                            subjectId,
                            jpegBuffers.toList(),
                        )
                        if (r.isSuccess) {
                            prefsRepository.addEnrolledSubject(subjectId)
                            withContext(Dispatchers.Main) {
                                busy = false
                                onRegistered()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                val err = r.exceptionOrNull()
                                message = if (err.isLikelyTimeout()) {
                                    "Request timed out. Server is likely still processing; please wait and check Enrolled users."
                                } else {
                                    err?.message.orEmpty()
                                }
                                busy = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Register") }
        }
    }
}

private fun Throwable?.isLikelyTimeout(): Boolean {
    if (this == null) return false
    if (this is SocketTimeoutException) return true
    val msg = this.message.orEmpty()
    if (msg.contains("timeout", ignoreCase = true)) return true
    return this.cause?.isLikelyTimeout() == true
}
