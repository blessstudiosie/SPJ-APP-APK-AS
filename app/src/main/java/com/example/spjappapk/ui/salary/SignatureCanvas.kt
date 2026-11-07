package com.example.spjappapk.ui.salary

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Canvas as AndroidCanvas
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

data class PathState(
    val path: Path,
    val strokeWidth: Float = 6f
)

@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    onSignatureChanged: (isEmpty: Boolean, bitmapProvider: () -> Bitmap?) -> Unit
) {
    val paths = remember { mutableStateListOf<PathState>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun provideBitmap(): Bitmap? {
        if (paths.isEmpty() || canvasSize.width <= 0 || canvasSize.height <= 0) return null

        val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        paths.forEach { pathState ->
            canvas.drawPath(pathState.path.asAndroidPath(), paint)
        }

        return bitmap
    }

    LaunchedEffect(paths.size) {
        onSignatureChanged(paths.isEmpty()) { provideBitmap() }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newPath = Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                            currentPath = newPath
                            paths.add(PathState(newPath))
                        },
                        onDrag = { change, _ ->
                            currentPath?.lineTo(change.position.x, change.position.y)
                            if (paths.isNotEmpty()) {
                                val last = paths.removeAt(paths.size - 1)
                                paths.add(last)
                            }
                        },
                        onDragEnd = {
                            currentPath = null
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        canvasSize = coordinates.size
                    }
            ) {
                paths.forEach { pathState ->
                    drawPath(
                        path = pathState.path,
                        color = Color.Black,
                        style = Stroke(
                            width = pathState.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (paths.isEmpty()) {
                Text(
                    text = "Tanda tangan di sini",
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            IconButton(
                onClick = {
                    paths.clear()
                    currentPath = null
                    onSignatureChanged(true) { null }
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Hapus Tanda Tangan", tint = Color.Gray)
            }
        }
    }
}

fun Bitmap.toBase64Png(): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}
