package app.ripple.mesh.ui

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.ripple.mesh.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * 100% Offline, CameraX-powered QR Code Scanner BottomSheet.
 *
 * Scans peer identity codes in real time using pure ZXing MultiFormatReader
 * and CameraX ImageAnalysis without requiring Google Play Services or internet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerModal(
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    var hasScanned by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.Black,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp)
                .background(Color.Black),
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val boxSize = minOf(width * 0.7f, 260f * context.resources.displayMetrics.density)

            // Camera Preview via CameraX
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val reader = MultiFormatReader().apply {
                            setHints(
                                mapOf(
                                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                                    DecodeHintType.TRY_HARDER to true,
                                )
                            )
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!hasScanned) {
                                        val result = processImage(imageProxy, reader)
                                        if (result != null && result.startsWith("RIPPLE-ID:v1:", ignoreCase = true)) {
                                            hasScanned = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onScanned(result)
                                                onDismiss()
                                            }
                                        }
                                    }
                                    imageProxy.close()
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis,
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            // Camera bind failed (e.g. no hardware camera)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Scanner Laser Animation
            val infiniteTransition = rememberInfiniteTransition(label = "scanLaser")
            val laserProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "laserOffset",
            )

            // Viewfinder Mask with Reticle and Laser
            Canvas(modifier = Modifier.fillMaxSize()) {
                val left = (size.width - boxSize) / 2f
                val top = (size.height - boxSize) / 2f - 20.dp.toPx()
                val rect = Rect(left, top, left + boxSize, top + boxSize)

                // Dark semi-transparent cutout
                val cutoutPath = Path().apply {
                    addRoundRect(RoundRect(rect, CornerRadius(16.dp.toPx())))
                }

                clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                    drawRect(Color.Black.copy(alpha = 0.65f))
                }

                // Focus Frame Outline
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                // Corner Reticles
                val cornerLength = 24.dp.toPx()
                val strokeWidth = 4.dp.toPx()
                val cornerColor = Color(0xFF64B5F6)

                // Top-Left
                drawLine(cornerColor, Offset(rect.left, rect.top + cornerLength), Offset(rect.left, rect.top + 8.dp.toPx()), strokeWidth, StrokeCap.Round)
                drawLine(cornerColor, Offset(rect.left, rect.top), Offset(rect.left + cornerLength, rect.top), strokeWidth, StrokeCap.Round)

                // Top-Right
                drawLine(cornerColor, Offset(rect.right - cornerLength, rect.top), Offset(rect.right, rect.top), strokeWidth, StrokeCap.Round)
                drawLine(cornerColor, Offset(rect.right, rect.top + 8.dp.toPx()), Offset(rect.right, rect.top + cornerLength), strokeWidth, StrokeCap.Round)

                // Bottom-Left
                drawLine(cornerColor, Offset(rect.left, rect.bottom - cornerLength), Offset(rect.left, rect.bottom), strokeWidth, StrokeCap.Round)
                drawLine(cornerColor, Offset(rect.left, rect.bottom), Offset(rect.left + cornerLength, rect.bottom), strokeWidth, StrokeCap.Round)

                // Bottom-Right
                drawLine(cornerColor, Offset(rect.right - cornerLength, rect.bottom), Offset(rect.right, rect.bottom), strokeWidth, StrokeCap.Round)
                drawLine(cornerColor, Offset(rect.right, rect.bottom - cornerLength), Offset(rect.right, rect.bottom), strokeWidth, StrokeCap.Round)

                // Animated Laser Line
                val laserY = rect.top + (rect.height * laserProgress)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF42A5F5), Color(0xFF90CAF9), Color(0xFF42A5F5), Color.Transparent),
                        startX = rect.left,
                        endX = rect.right,
                    ),
                    topLeft = Offset(rect.left + 8.dp.toPx(), laserY - 1.5.dp.toPx()),
                    size = Size(rect.width - 16.dp.toPx(), 3.dp.toPx()),
                )
            }

            // Top Bar Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = Color.White)
                    }

                    Text(
                        text = "Scan Peer QR Code",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            cameraControl?.enableTorch(isFlashOn)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashOn) Color.Yellow else Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 24.dp),
                ) {
                    Text(
                        text = "Point camera at contact's QR code",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * Extracts Y-plane luminance from ImageProxy and decodes QR matrix with ZXing.
 */
private fun processImage(image: ImageProxy, reader: MultiFormatReader): String? {
    return try {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride

        val source = PlanarYUVLuminanceSource(
            data,
            rowStride,
            height,
            0,
            0,
            width,
            height,
            false,
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = reader.decodeWithState(binaryBitmap)
        result.text
    } catch (e: Exception) {
        null
    } finally {
        reader.reset()
    }
}
