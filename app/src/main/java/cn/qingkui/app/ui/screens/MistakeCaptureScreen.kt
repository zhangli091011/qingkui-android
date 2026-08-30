package cn.qingkui.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import cn.qingkui.app.data.local.MistakeImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun MistakeCaptureScreen(
    onClose: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var manualEntry by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var capturing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if (!it) error = "未授予相机权限，可从相册选择图片"
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val directory = File(context.filesDir, "mistake-images").apply { mkdirs() }
                val target = File(directory, "${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "无法读取所选图片" }
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                imagePath = target.absolutePath
                error = null
            }.onFailure { error = it.message ?: "无法读取所选图片" }
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    if (imagePath != null || manualEntry) {
        MistakeImageEditor(
            imagePath = imagePath,
            onRetake = {
                imagePath = null
                manualEntry = false
            },
            onClose = onClose,
            onSave = onSave,
        )
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "关闭") }
            Text("拍摄错题", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = "从相册选择")
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
            if (permissionGranted) {
                AndroidView(
                    factory = { previewContext ->
                        PreviewView(previewContext).also { view ->
                            view.scaleType = PreviewView.ScaleType.FILL_CENTER
                            val future = ProcessCameraProvider.getInstance(previewContext)
                            future.addListener({
                                runCatching {
                                    val provider = future.get()
                                    val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                                    cameraProvider = provider
                                    imageCapture = capture
                                }.onFailure { error = "相机启动失败：${it.message}" }
                            }, ContextCompat.getMainExecutor(previewContext))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("相机权限不可用", color = Color.White)
                    OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("重新授权") }
                }
            }
            if (capturing) CircularProgressIndicator(color = Color.White)
        }
        if (error != null) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        Row(
            Modifier.fillMaxWidth().height(104.dp).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("相册")
            }
            OutlinedButton(onClick = { manualEntry = true }) {
                Icon(Icons.Outlined.EditNote, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("手动输入")
            }
            Button(
                enabled = imageCapture != null && !capturing,
                onClick = {
                    val capture = imageCapture ?: return@Button
                    val directory = File(context.filesDir, "mistake-images").apply { mkdirs() }
                    val target = File(directory, "${UUID.randomUUID()}.jpg")
                    capturing = true
                    capture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(target).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                capturing = false
                                imagePath = target.absolutePath
                            }

                            override fun onError(exception: ImageCaptureException) {
                                capturing = false
                                error = "拍照失败：${exception.message}"
                            }
                        },
                    )
                },
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("拍照")
            }
        }
    }
}

@Composable
private fun MistakeImageEditor(
    imagePath: String?,
    onRetake: () -> Unit,
    onClose: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("数学") }
    var question by remember { mutableStateOf("") }
    var studentWork by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("分析错因并给出同类练习") }
    var errorCategory by remember { mutableStateOf("method") }
    var rotationDegrees by remember { mutableStateOf(0) }
    var cropInsetFraction by remember { mutableStateOf(0f) }
    var processing by remember { mutableStateOf(false) }
    var processingError by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {
                imagePath?.let { File(it).delete() }
                onClose()
            }) { Icon(Icons.Outlined.Close, contentDescription = "关闭") }
            Text("确认错题", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = {
                imagePath?.let { File(it).delete() }
                onRetake()
            }) { Icon(Icons.Outlined.Replay, contentDescription = "重拍") }
        }
        if (imagePath != null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val cropScale = 1f / (1f - cropInsetFraction * 2f).coerceAtLeast(0.6f)
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "错题图片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            rotationZ = rotationDegrees.toFloat(),
                            scaleX = cropScale,
                            scaleY = cropScale,
                            clip = true,
                        ),
                )
                if (processing) CircularProgressIndicator()
            }
        }
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (imagePath != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { rotationDegrees = (rotationDegrees - 90).mod(360) },
                        enabled = !processing,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.RotateLeft, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("左旋")
                    }
                    OutlinedButton(
                        onClick = { rotationDegrees = (rotationDegrees + 90).mod(360) },
                        enabled = !processing,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.RotateRight, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("右旋")
                    }
                }
                Text("裁剪边缘 ${(cropInsetFraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = cropInsetFraction,
                    onValueChange = { cropInsetFraction = it },
                    valueRange = 0f..0.2f,
                    steps = 7,
                    enabled = !processing,
                )
            }
            OutlinedTextField(subject, { subject = it }, label = { Text("学科") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(question, { question = it }, label = { Text("题目文字（可留空交给 OCR）") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(studentWork, { studentWork = it }, label = { Text("我的作答过程") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Text("错因类型", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "concept" to "概念",
                    "reading" to "审题",
                    "method" to "方法",
                    "calculation" to "计算",
                    "expression" to "表达",
                ).forEach { (value, label) ->
                    if (errorCategory == value) {
                        Button(onClick = { errorCategory = value }) { Text(label) }
                    } else {
                        OutlinedButton(onClick = { errorCategory = value }) { Text(label) }
                    }
                }
            }
            OutlinedTextField(goal, { goal = it }, label = { Text("希望重点分析") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(
                onClick = {
                    processing = true
                    processingError = null
                    scope.launch {
                        runCatching {
                            imagePath?.let {
                                withContext(Dispatchers.IO) {
                                    MistakeImageProcessor.process(it, rotationDegrees, cropInsetFraction)
                                }
                            }.orEmpty()
                        }.onSuccess { processedPath ->
                            if (imagePath != null && processedPath != imagePath) File(imagePath).delete()
                            onSave(
                                processedPath,
                                subject.trim().ifBlank { "待识别" },
                                question,
                                studentWork,
                                goal,
                                errorCategory,
                            )
                        }.onFailure {
                            processing = false
                            processingError = it.message ?: "图片处理失败"
                        }
                    }
                },
                enabled = !processing && (imagePath != null || question.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(if (processing) "正在处理图片" else "保存并开始识别") }
            processingError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
