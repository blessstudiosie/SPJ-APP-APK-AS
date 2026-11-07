package com.example.spjappapk.ui.attendance

import android.Manifest
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.spjappapk.data.remote.SupabaseClient
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.util.DateTimeUtil
import com.example.spjappapk.util.FileUtil
import com.example.spjappapk.util.LocationHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.*
import java.util.concurrent.Executors

enum class ScannerPhase {
    QR_SCANNING,
    SELFIE_CAMERA,
    UPLOADING,
    SUCCESS
}

@kotlin.OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun AttendanceScannerScreen(
    currentUser: SalesPerson?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    var phase by remember { mutableStateOf(ScannerPhase.QR_SCANNING) }
    var qrContent by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("Arahkan kamera belakang ke QR Code Absensi") }
    var isUploading by remember { mutableStateOf(false) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Absensi QR & Selfie") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (phase != ScannerPhase.QR_SCANNING) {
                        IconButton(onClick = {
                            phase = ScannerPhase.QR_SCANNING
                            qrContent = null
                            statusMessage = "Arahkan kamera belakang ke QR Code Absensi"
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Ulangi QR")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (!permissionsState.allPermissionsGranted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Izin Kamera & Lokasi Diperlukan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aplikasi membutuhkan izin kamera untuk memindai QR & foto selfie, serta izin GPS untuk memvalidasi posisi Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                        Text("Berikan Izin Akses")
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (phase) {
                    ScannerPhase.QR_SCANNING, ScannerPhase.SELFIE_CAMERA -> {
                        // Camera Preview View
                        CameraView(
                            phase = phase,
                            onQrDetected = { scannedText ->
                                if (validateQrFormat(scannedText)) {
                                    qrContent = scannedText
                                    phase = ScannerPhase.SELFIE_CAMERA
                                    statusMessage = "QR Valid! Sekarang ambil foto selfie Anda."
                                    Toast.makeText(context, "QR Code Valid!", Toast.LENGTH_SHORT).show()
                                } else {
                                    statusMessage = "QR Code tidak dikenal! Format harus SPJ_ATTENDANCE|..."
                                }
                            },
                            onImageCaptureCreated = { ic ->
                                imageCapture = ic
                            }
                        )

                        // Top Overlay Instruction Banner
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (phase == ScannerPhase.QR_SCANNING) Icons.Default.QrCodeScanner else Icons.Default.Camera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Bottom Action Controls
                        if (phase == ScannerPhase.SELFIE_CAMERA) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FloatingActionButton(
                                    onClick = {
                                        val ic = imageCapture
                                        if (ic != null && currentUser != null) {
                                            scope.launch {
                                                isUploading = true
                                                phase = ScannerPhase.UPLOADING
                                                takeSelfieAndSubmit(
                                                    context = context,
                                                    imageCapture = ic,
                                                    currentUser = currentUser,
                                                    locationHelper = locationHelper,
                                                    onSuccess = {
                                                        isUploading = false
                                                        phase = ScannerPhase.SUCCESS
                                                    },
                                                    onError = { err ->
                                                        isUploading = false
                                                        phase = ScannerPhase.SELFIE_CAMERA
                                                        statusMessage = "Gagal: $err"
                                                        Toast.makeText(context, "Gagal: $err", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            }
                                        } else {
                                            Toast.makeText(context, "Kamera selfie belum siap", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(Icons.Default.Camera, contentDescription = "Ambil Foto", modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }

                    ScannerPhase.UPLOADING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Memproses Absensi...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Mengambil koordinat GPS & mengunggah foto selfie ke server Supabase.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    ScannerPhase.SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Absensi Berhasil Dicatat!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Data kehadiran dan foto selfie Anda telah tersimpan di server Supabase.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = onNavigateBack,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text("Kembali ke Dashboard")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Validasi Format QR Code: SPJ_ATTENDANCE|{Timestamp}|{RandomHash}
 */
fun validateQrFormat(text: String): Boolean {
    if (!text.startsWith("SPJ_ATTENDANCE|")) return false
    val parts = text.split("|")
    return parts.size >= 3 && parts[0] == "SPJ_ATTENDANCE"
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraView(
    phase: ScannerPhase,
    onQrDetected: (String) -> Unit,
    onImageCaptureCreated: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraSelector = if (phase == ScannerPhase.QR_SCANNING) {
        CameraSelector.DEFAULT_BACK_CAMERA
    } else {
        CameraSelector.DEFAULT_FRONT_CAMERA
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                onImageCaptureCreated(imageCapture)

                cameraProvider.unbindAll()

                if (phase == ScannerPhase.QR_SCANNING) {
                    val barcodeScanner = BarcodeScanning.getClient()
                    val executor = Executors.newSingleThreadExecutor()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNull_or_empty_custom()) {
                                            onQrDetected(rawValue!!)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun String?.isNull_or_empty_custom(): Boolean {
    return this == null || this.isEmpty()
}

/**
 * Pengambilan Foto Selfie, Koordinat GPS, Upload Storage, dan Insert PostgREST
 */
suspend fun takeSelfieAndSubmit(
    context: Context,
    imageCapture: ImageCapture,
    currentUser: SalesPerson,
    locationHelper: LocationHelper,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        // 1. Ambil Foto Selfie ke Temp File
        val photoFile = FileUtil.createImageFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        val photoTaken = suspendCancellableCoroutine<Boolean> { continuation ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(true, null)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("AttendanceScanner", "Photo capture failed: ${exception.message}", exception)
                        continuation.resume(false, null)
                    }
                }
            )
        }

        if (!photoTaken || !photoFile.exists()) {
            onError("Gagal mengambil foto selfie.")
            return@withContext
        }

        // 2. Ambil Lokasi GPS
        val location = locationHelper.getCurrentLocation()
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0

        // 3. Upload Selfie File ke Supabase Storage (Bucket: attendance_selfies)
        val fileName = "selfie_${currentUser.id}_${System.currentTimeMillis()}.jpg"
        val bytes = photoFile.readBytes()

        SupabaseClient.client.storage["attendance_selfies"].upload(fileName, bytes) {
            upsert = true
        }

        val selfieUrl = SupabaseClient.client.storage["attendance_selfies"].publicUrl(fileName)

        // 4. Insert Record ke Supabase PostgREST Table: attendance_logs
        val checkTimeIso = DateTimeUtil.getUtcIsoFormat().format(Date())
        val payload = buildJsonObject {
            put("employee_id", currentUser.id)
            put("check_time", checkTimeIso)
            put("type", "IN")
            put("selfie_url", selfieUrl)
            put("latitude", lat)
            put("longitude", lng)
        }

        SupabaseClient.client.postgrest["attendance_logs"].insert(payload)

        withContext(Dispatchers.Main) {
            onSuccess()
        }
    } catch (e: Exception) {
        Log.e("AttendanceScanner", "Error in attendance submission", e)
        withContext(Dispatchers.Main) {
            onError(e.localizedMessage ?: "Terjadi kesalahan saat memproses absensi.")
        }
    }
}
