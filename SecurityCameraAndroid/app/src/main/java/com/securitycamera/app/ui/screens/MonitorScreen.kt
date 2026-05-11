package com.securitycamera.app.ui.screens

import android.content.Context
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.securitycamera.app.BuildConfig
import com.securitycamera.app.camera.awaitProcessCameraProvider
import com.securitycamera.app.camera.boundingBoxCenterInsideRoi
import com.securitycamera.app.camera.roiOverlayFitCenterWithRotation
import com.securitycamera.app.camera.roiRectNormalized
import com.securitycamera.app.camera.toBitmapRgba
import com.securitycamera.app.camera.toJpegBytes
import com.securitycamera.app.data.AppSettings
import com.securitycamera.app.data.FunctionsApi
import com.securitycamera.app.data.roiCoversFullNormalizedFrame
import com.securitycamera.app.data.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val COOLDOWN_MS = 90_000L
private const val THROTTLE_MS = 400L
private const val STABLE_FRAMES = 2
private const val PERSON_LABEL_CONF = 0.35f
private const val PERSON_SCENE_LABEL_CONF = 0.55f

private data class AnalysisFrameMeta(val width: Int, val height: Int, val rotationDegrees: Int)

@Suppress("DEPRECATION")
private fun defaultDisplayRotation(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return when (wm.defaultDisplay.rotation) {
        Surface.ROTATION_0 -> Surface.ROTATION_0
        Surface.ROTATION_90 -> Surface.ROTATION_90
        Surface.ROTATION_180 -> Surface.ROTATION_180
        Surface.ROTATION_270 -> Surface.ROTATION_270
        else -> Surface.ROTATION_0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    prefsRepository: PrefsRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settings by prefsRepository.settings.collectAsState(initial = AppSettings())
    val settingsState = rememberUpdatedState(settings)

    var camGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> camGranted = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    var armed by remember { mutableStateOf(true) }
    var statusLine by remember { mutableStateOf("Idle") }
    var analyzing by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var analysisFrameMeta by remember { mutableStateOf<AnalysisFrameMeta?>(null) }
    val surfaceRotationForCapture = remember(context) { defaultDisplayRotation(context) }
    val imageCapture = remember(surfaceRotationForCapture) {
        ImageCapture.Builder()
            .setTargetRotation(surfaceRotationForCapture)
            .build()
    }
    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build(),
        )
    }
    val objectDetector = remember {
        val opts = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(opts)
    }
    val imageLabeler = remember {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }
    val api = remember { FunctionsApi() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    val stableCount = remember { AtomicInteger(0) }
    val lastThrottle = remember { AtomicLong(0L) }
    val capturing = remember { AtomicBoolean(false) }

    LaunchedEffect(camGranted, armed, lifecycleOwner) {
        if (!camGranted || !armed) return@LaunchedEffect
        val cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val provider = context.awaitProcessCameraProvider()
        provider.unbindAll()

        val surfaceRotation = defaultDisplayRotation(context)
        val preview = Preview.Builder()
            .setTargetRotation(surfaceRotation)
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetRotation(surfaceRotation)
            .build()

        var lastPostedW = 0
        var lastPostedH = 0
        var lastPostedRot = Int.MIN_VALUE
        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val w = imageProxy.width
            val h = imageProxy.height
            val rot = imageProxy.imageInfo.rotationDegrees
            if (w != lastPostedW || h != lastPostedH || rot != lastPostedRot) {
                lastPostedW = w
                lastPostedH = h
                lastPostedRot = rot
                mainExecutor.execute {
                    analysisFrameMeta = AnalysisFrameMeta(w, h, rot)
                }
            }
            analyzeFrame(
                imageProxy = imageProxy,
                settingsState = settingsState,
                faceDetector = faceDetector,
                objectDetector = objectDetector,
                imageLabeler = imageLabeler,
                stableCount = stableCount,
                lastThrottle = lastThrottle,
                capturing = capturing,
                onTriggerCapture = {
                    triggerAnalyzePipeline(
                        executor = mainExecutor,
                        imageCapture = imageCapture,
                        prefsRepository = prefsRepository,
                        api = api,
                        settings = settingsState.value,
                        scope = scope,
                        capturingFlag = capturing,
                        onBusy = { analyzing = it },
                        onStatus = { statusLine = it },
                    )
                },
            )
        }

        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis,
            imageCapture,
        )

        try {
            awaitCancellation()
        } finally {
            provider.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitor") },
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
                .padding(16.dp),
        ) {
            Text(
                if (settings.roiCoversFullNormalizedFrame()) {
                    "Stable person (labels/object) or face in frame triggers analyze " +
                        "(cooldown ${COOLDOWN_MS / 1000} s)"
                } else {
                    "Stable person (labels/object) or face in ROI triggers analyze " +
                        "(cooldown ${COOLDOWN_MS / 1000} s)"
                },
            )
            if (settings.notifyEmail.isBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Notify email in Settings is empty. If the server has no enrolled faces, " +
                        "alert emails are skipped entirely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Monitoring")
                Spacer(Modifier.width(12.dp))
                Switch(checked = armed, onCheckedChange = { armed = it })
            }
            Spacer(Modifier.height(8.dp))
            Text(statusLine, style = MaterialTheme.typography.bodyMedium)
            if (analyzing) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            if (!camGranted) {
                Text("Camera permission is required.", color = MaterialTheme.colorScheme.error)
                return@Column
            }
            val s = settings
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                )
                if (!s.roiCoversFullNormalizedFrame()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val meta = analysisFrameMeta
                        val (topLeft, rectSize) =
                            if (meta != null && meta.width > 0 && meta.height > 0) {
                                roiOverlayFitCenterWithRotation(
                                    s,
                                    size.width,
                                    size.height,
                                    meta.width,
                                    meta.height,
                                    meta.rotationDegrees,
                                )
                            } else {
                                val w = size.width
                                val h = size.height
                                Offset(s.roiLeft * w, s.roiTop * h) to Size(s.roiWidth * w, s.roiHeight * h)
                            }
                        drawRect(
                            color = Color(0x66FF9800),
                            topLeft = topLeft,
                            size = rectSize,
                        )
                    }
                }
            }
        }
    }
}

private fun analyzeFrame(
    imageProxy: ImageProxy,
    settingsState: State<AppSettings>,
    faceDetector: FaceDetector,
    objectDetector: ObjectDetector,
    imageLabeler: ImageLabeler,
    stableCount: AtomicInteger,
    lastThrottle: AtomicLong,
    capturing: AtomicBoolean,
    onTriggerCapture: () -> Unit,
) {
    try {
        val now = SystemClock.elapsedRealtime()
        if (now - lastThrottle.get() < THROTTLE_MS) return
        lastThrottle.set(now)

        val bmp = imageProxy.toBitmapRgba()
        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = InputImage.fromBitmap(bmp, rotation)
        val faces = runBlocking {
            faceDetector.process(input).await()
        }
        val objects: List<DetectedObject> = runBlocking {
            objectDetector.process(input).await()
        }
        val imageLabels: List<ImageLabel> = runBlocking {
            imageLabeler.process(input).await()
        }

        val s = settingsState.value
        val roi = roiRectNormalized(s, bmp.width, bmp.height)
        val faceInRoi = faces.any { boundingBoxCenterInsideRoi(it.boundingBox, roi) }
        val personInRoi = objects.any { detected: DetectedObject ->
            val looksPerson = detected.labels.any { label ->
                label.confidence >= PERSON_LABEL_CONF &&
                    isLikelyPersonObjectLabel(label.text)
            }
            looksPerson && boundingBoxCenterInsideRoi(detected.boundingBox, roi)
        }
        val scenePerson =
            imageLabels.any { label: ImageLabel ->
                label.confidence >= PERSON_SCENE_LABEL_CONF &&
                    isLikelyPersonObjectLabel(label.text)
            }
        val hit = personInRoi || faceInRoi || scenePerson

        if (hit) {
            val n = stableCount.incrementAndGet()
            if (n >= STABLE_FRAMES) {
                stableCount.set(0)
                if (capturing.compareAndSet(false, true)) {
                    onTriggerCapture()
                }
            }
        } else {
            stableCount.set(0)
        }
    } catch (_: Exception) {
        stableCount.set(0)
    } finally {
        imageProxy.close()
    }
}

private fun triggerAnalyzePipeline(
    executor: Executor,
    imageCapture: ImageCapture,
    prefsRepository: PrefsRepository,
    api: FunctionsApi,
    settings: AppSettings,
    scope: CoroutineScope,
    capturingFlag: AtomicBoolean,
    onBusy: (Boolean) -> Unit,
    onStatus: (String) -> Unit,
) {
    if (settings.baseUrl.isBlank() || settings.functionKey.isBlank()) {
        onStatus(
            if (BuildConfig.DEBUG) {
                "Server not configured. Add functions.base.url and functions.key to local.properties, then rebuild."
            } else {
                "Cannot reach the server. Try again later."
            },
        )
        capturingFlag.set(false)
        return
    }

    scope.launch(Dispatchers.IO) {
        val last = prefsRepository.getLastAnalyzeMs()
        if (System.currentTimeMillis() - last < COOLDOWN_MS) {
            withContext(Dispatchers.Main) {
                onStatus(
                    "Cooldown: ~${(COOLDOWN_MS - (System.currentTimeMillis() - last)) / 1000} s left",
                )
                capturingFlag.set(false)
            }
            return@launch
        }

        withContext(Dispatchers.Main) { onBusy(true) }

        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = image.toJpegBytes()
                    image.close()
                    scope.launch(Dispatchers.IO) {
                        try {
                            val r = api.analyze(
                                settings.baseUrl,
                                settings.functionKey,
                                bytes,
                                settings.notifyEmail,
                            )
                            val json = r.getOrNull()
                            if (json != null) {
                                prefsRepository.markAnalyzeAttempt()
                                val reg = json.optBoolean("registered", false)
                                val sid = json.optString("subject_id", "")
                                withContext(Dispatchers.Main) {
                                    if (reg) {
                                        onStatus("Registered: $sid")
                                    } else {
                                        val reason = json.optString("reason", "")
                                        val notified = json.optBoolean("notified", false)
                                        val mailErr = json.optString("mailError", "")
                                        val mailSkipped =
                                            json.optString("mailSkippedReason", "")
                                        val tail = buildString {
                                            if (reason.isNotBlank()) append(" ($reason)")
                                            if (notified) append(" · Email sent")
                                            if (mailSkipped.isNotBlank()) {
                                                append(" · ")
                                                append(formatMailSkippedReason(mailSkipped))
                                            }
                                            if (mailErr.isNotBlank()) append(" mail:$mailErr")
                                        }
                                        onStatus("Not registered$tail")
                                    }
                                }
                            } else {
                                val err = r.exceptionOrNull()
                                withContext(Dispatchers.Main) {
                                    onStatus("API error: ${err?.message}")
                                }
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                onBusy(false)
                                capturingFlag.set(false)
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    scope.launch(Dispatchers.Main) {
                        onStatus("Capture failed: ${exception.message}")
                        onBusy(false)
                        capturingFlag.set(false)
                    }
                }
            },
        )
    }
}

private fun isLikelyPersonObjectLabel(text: String): Boolean {
    val t = text.trim().lowercase()
    if (t.contains("person") || t.contains("human") || t.contains("people")) return true
    return t == "man" || t == "woman" || t == "boy" || t == "girl"
}

private fun formatMailSkippedReason(reason: String): String =
    when (reason) {
        "no_enrollments_and_mail_not_configured" ->
            "no alerts (no enrolled faces & mail not configured)"
        "notify_not_configured" ->
            "mail skipped (ACS/sender/recipient not configured)"
        else -> reason
    }
