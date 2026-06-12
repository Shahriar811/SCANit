package com.example.scanit.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.scanit.util.ImageProcessor
import com.example.scanit.ui.common.IosBackButton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

enum class DragTarget {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_EDGE,
    BOTTOM_EDGE,
    LEFT_EDGE,
    RIGHT_EDGE,
    BODY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    imageUri: Uri,
    onNavigateBack: () -> Unit,
    onCropCompleted: (Uri) -> Unit
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    var cropRect by remember { mutableStateOf<CropRect?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var activeDragTarget by remember { mutableStateOf(DragTarget.NONE) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartCropRect by remember { mutableStateOf<CropRect?>(null) }
    var magnifierBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        imageBitmap = ImageProcessor.loadBitmap(context, imageUri)
        imageBitmap?.let {
            imageSize = Size(it.width.toFloat(), it.height.toFloat())
        }
    }

    val density = LocalDensity.current
    var imageOffset by remember { mutableStateOf(Offset.Zero) }
    var imageDisplaySize by remember { mutableStateOf(Size.Zero) }

    // Initialize CropRect with a default 10% margins once image display size is computed
    LaunchedEffect(imageDisplaySize) {
        if (imageDisplaySize.width > 0 && imageDisplaySize.height > 0 && cropRect == null) {
            val marginX = imageDisplaySize.width * 0.1f
            val marginY = imageDisplaySize.height * 0.1f
            cropRect = CropRect(
                left = marginX,
                top = marginY,
                right = imageDisplaySize.width - marginX,
                bottom = imageDisplaySize.height - marginY
            )
        }
    }

    // Update magnifier bitmap when dragging a handle
    LaunchedEffect(isDragging, cropRect, activeDragTarget) {
        val bitmap = imageBitmap
        val rect = cropRect
        if (isDragging && bitmap != null && rect != null && activeDragTarget != DragTarget.NONE && activeDragTarget != DragTarget.BODY) {
            // Determine active handle location relative to image display space
            val handlePoint = when (activeDragTarget) {
                DragTarget.TOP_LEFT -> Offset(rect.left, rect.top)
                DragTarget.TOP_RIGHT -> Offset(rect.right, rect.top)
                DragTarget.BOTTOM_LEFT -> Offset(rect.left, rect.bottom)
                DragTarget.BOTTOM_RIGHT -> Offset(rect.right, rect.bottom)
                DragTarget.TOP_EDGE -> Offset((rect.left + rect.right) / 2f, rect.top)
                DragTarget.BOTTOM_EDGE -> Offset((rect.left + rect.right) / 2f, rect.bottom)
                DragTarget.LEFT_EDGE -> Offset(rect.left, (rect.top + rect.bottom) / 2f)
                DragTarget.RIGHT_EDGE -> Offset(rect.right, (rect.top + rect.bottom) / 2f)
                else -> null
            }
            if (handlePoint != null) {
                // Convert touch point to original bitmap coordinate
                val scale = bitmap.width.toFloat() / imageDisplaySize.width
                val px = (handlePoint.x * scale).toInt()
                val py = (handlePoint.y * scale).toInt()
                
                // Crop a small square from high-res bitmap
                val size = 180
                val left = (px - size / 2).coerceIn(0, bitmap.width - size.coerceAtMost(bitmap.width))
                val top = (py - size / 2).coerceIn(0, bitmap.height - size.coerceAtMost(bitmap.height))
                val right = (left + size).coerceAtMost(bitmap.width)
                val bottom = (top + size).coerceAtMost(bitmap.height)
                
                if (right > left && bottom > top) {
                    magnifierBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                }
            }
        } else {
            magnifierBitmap = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crop & Align",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IosBackButton(onClick = onNavigateBack)
                },
                actions = {
                    IconButton(
                        onClick = {
                            imageBitmap?.let { bitmap ->
                                cropRect?.let { rect ->
                                    val scale = imageDisplaySize.width / bitmap.width.toFloat()
                                    val left = (rect.left / scale).toInt().coerceIn(0, bitmap.width)
                                    val top = (rect.top / scale).toInt().coerceIn(0, bitmap.height)
                                    val right = (rect.right / scale).toInt().coerceIn(0, bitmap.width)
                                    val bottom = (rect.bottom / scale).toInt().coerceIn(0, bitmap.height)

                                    if (right > left && bottom > top) {
                                        val croppedBitmap = ImageProcessor.cropBitmap(
                                            bitmap,
                                            left,
                                            top,
                                            right,
                                            bottom
                                        )
                                        val croppedUri = ImageProcessor.saveAdjustedBitmap(
                                            context,
                                            croppedBitmap,
                                            imageUri
                                        )
                                        onCropCompleted(croppedUri)
                                    } else {
                                        onCropCompleted(imageUri)
                                    }
                                } ?: onCropCompleted(imageUri)
                            }
                        },
                        enabled = cropRect != null
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Apply Crop",
                            tint = if (cropRect != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            imageBitmap?.let { bitmap ->
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val maxWidth = constraints.maxWidth.toFloat()
                    val maxHeight = constraints.maxHeight.toFloat()
                    val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val containerAspectRatio = maxWidth / maxHeight

                    val (displayWidth, displayHeight) = if (imageAspectRatio > containerAspectRatio) {
                        Pair(maxWidth, maxWidth / imageAspectRatio)
                    } else {
                        Pair(maxHeight * imageAspectRatio, maxHeight)
                    }

                    imageDisplaySize = Size(displayWidth, displayHeight)
                    imageOffset = Offset(
                        (maxWidth - displayWidth) / 2f,
                        (maxHeight - displayHeight) / 2f
                    )

                    // Displayed image
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Image to crop",
                        modifier = Modifier
                            .size(
                                with(density) { displayWidth.toDp() },
                                with(density) { displayHeight.toDp() }
                            )
                            .offset(
                                x = with(density) { imageOffset.x.toDp() },
                                y = with(density) { imageOffset.y.toDp() }
                            ),
                        contentScale = ContentScale.Fit
                    )

                    // Touch thresholds and hit testing
                    val touchThreshold = with(density) { 24.dp.toPx() }
                    val minSize = with(density) { 48.dp.toPx() } // Minimum crop box size to prevent collapsing

                    // Interactive canvas overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(imageDisplaySize, cropRect) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val rect = cropRect ?: return@detectDragGestures
                                        // Adjust offset to be relative to the displayed image box
                                        val relativeOffset = offset - imageOffset
                                        
                                        // Hit test to see what we are dragging
                                        activeDragTarget = getDragTarget(relativeOffset, rect, touchThreshold)
                                        if (activeDragTarget != DragTarget.NONE) {
                                            isDragging = true
                                            dragStartOffset = offset
                                            dragStartCropRect = rect
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        val rect = dragStartCropRect ?: return@detectDragGestures
                                        val startOffset = dragStartOffset
                                        val target = activeDragTarget
                                        if (target == DragTarget.NONE) return@detectDragGestures
                                        
                                        val totalDelta = change.position - startOffset
                                        
                                        var newLeft = rect.left
                                        var newRight = rect.right
                                        var newTop = rect.top
                                        var newBottom = rect.bottom
                                        
                                        when (target) {
                                            DragTarget.TOP_LEFT -> {
                                                newLeft = (rect.left + totalDelta.x).coerceIn(0f, rect.right - minSize)
                                                newTop = (rect.top + totalDelta.y).coerceIn(0f, rect.bottom - minSize)
                                            }
                                            DragTarget.TOP_RIGHT -> {
                                                newRight = (rect.right + totalDelta.x).coerceIn(rect.left + minSize, displayWidth)
                                                newTop = (rect.top + totalDelta.y).coerceIn(0f, rect.bottom - minSize)
                                            }
                                            DragTarget.BOTTOM_LEFT -> {
                                                newLeft = (rect.left + totalDelta.x).coerceIn(0f, rect.right - minSize)
                                                newBottom = (rect.bottom + totalDelta.y).coerceIn(rect.top + minSize, displayHeight)
                                            }
                                            DragTarget.BOTTOM_RIGHT -> {
                                                newRight = (rect.right + totalDelta.x).coerceIn(rect.left + minSize, displayWidth)
                                                newBottom = (rect.bottom + totalDelta.y).coerceIn(rect.top + minSize, displayHeight)
                                            }
                                            DragTarget.TOP_EDGE -> {
                                                newTop = (rect.top + totalDelta.y).coerceIn(0f, rect.bottom - minSize)
                                            }
                                            DragTarget.BOTTOM_EDGE -> {
                                                newBottom = (rect.bottom + totalDelta.y).coerceIn(rect.top + minSize, displayHeight)
                                            }
                                            DragTarget.LEFT_EDGE -> {
                                                newLeft = (rect.left + totalDelta.x).coerceIn(0f, rect.right - minSize)
                                            }
                                            DragTarget.RIGHT_EDGE -> {
                                                newRight = (rect.right + totalDelta.x).coerceIn(rect.left + minSize, displayWidth)
                                            }
                                            DragTarget.BODY -> {
                                                val dx = totalDelta.x
                                                val dy = totalDelta.y
                                                val width = rect.right - rect.left
                                                val height = rect.bottom - rect.top
                                                
                                                newLeft = (rect.left + dx).coerceIn(0f, displayWidth - width)
                                                newRight = newLeft + width
                                                newTop = (rect.top + dy).coerceIn(0f, displayHeight - height)
                                                newBottom = newTop + height
                                            }
                                            else -> {}
                                        }
                                        cropRect = CropRect(newLeft, newTop, newRight, newBottom)
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        activeDragTarget = DragTarget.NONE
                                        dragStartCropRect = null
                                    }
                                )
                            }
                    ) {
                        cropRect?.let { rect ->
                            // 1. Darken outside crop area
                            val path = Path().apply {
                                addRect(Rect(Offset.Zero, size))
                                addRect(
                                    Rect(
                                        Offset(rect.left + imageOffset.x, rect.top + imageOffset.y),
                                        Size(rect.right - rect.left, rect.bottom - rect.top)
                                    )
                                )
                            }
                            drawPath(
                                path = path,
                                color = Color.Black.copy(alpha = 0.55f)
                            )

                            // 2. Crop rectangle border
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(rect.left + imageOffset.x, rect.top + imageOffset.y),
                                size = Size(rect.right - rect.left, rect.bottom - rect.top),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // 3. Draw Rule-of-Thirds Grid while dragging
                            if (isDragging) {
                                val thirdWidth = (rect.right - rect.left) / 3f
                                val thirdHeight = (rect.bottom - rect.top) / 3f
                                
                                // Vertical lines
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(rect.left + imageOffset.x + thirdWidth, rect.top + imageOffset.y),
                                    end = Offset(rect.left + imageOffset.x + thirdWidth, rect.bottom + imageOffset.y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(rect.left + imageOffset.x + thirdWidth * 2, rect.top + imageOffset.y),
                                    end = Offset(rect.left + imageOffset.x + thirdWidth * 2, rect.bottom + imageOffset.y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                // Horizontal lines
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(rect.left + imageOffset.x, rect.top + imageOffset.y + thirdHeight),
                                    end = Offset(rect.right + imageOffset.x, rect.top + imageOffset.y + thirdHeight),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(rect.left + imageOffset.x, rect.top + imageOffset.y + thirdHeight * 2),
                                    end = Offset(rect.right + imageOffset.x, rect.top + imageOffset.y + thirdHeight * 2),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // 4. Draw Corner Handles (Clean iOS-style visual outline)
                            val corners = listOf(
                                Offset(rect.left + imageOffset.x, rect.top + imageOffset.y),
                                Offset(rect.right + imageOffset.x, rect.top + imageOffset.y),
                                Offset(rect.left + imageOffset.x, rect.bottom + imageOffset.y),
                                Offset(rect.right + imageOffset.x, rect.bottom + imageOffset.y)
                            )
                            corners.forEach { corner ->
                                // Outer outline shadow
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    radius = 9.dp.toPx(),
                                    center = corner
                                )
                                // Inner white circle
                                drawCircle(
                                    color = Color.White,
                                    radius = 8.dp.toPx(),
                                    center = corner
                                )
                                // Core highlight
                                drawCircle(
                                    color = Color(0xFF007AFF), // iOS blue
                                    radius = 3.dp.toPx(),
                                    center = corner
                                )
                            }

                            // 5. Draw Edge Midpoint Handles (Sleek pills)
                            val edges = listOf(
                                Pair(Offset((rect.left + rect.right) / 2f + imageOffset.x, rect.top + imageOffset.y), true), // Horizontal top
                                Pair(Offset((rect.left + rect.right) / 2f + imageOffset.x, rect.bottom + imageOffset.y), true), // Horizontal bottom
                                Pair(Offset(rect.left + imageOffset.x, (rect.top + rect.bottom) / 2f + imageOffset.y), false), // Vertical left
                                Pair(Offset(rect.right + imageOffset.x, (rect.top + rect.bottom) / 2f + imageOffset.y), false) // Vertical right
                            )
                            edges.forEach { (midpoint, isHorizontal) ->
                                val pillLen = 16.dp.toPx()
                                val pillThickness = 3.dp.toPx()
                                if (isHorizontal) {
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(midpoint.x - pillLen / 2, midpoint.y - pillThickness / 2),
                                        size = Size(pillLen, pillThickness),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillThickness / 2)
                                    )
                                } else {
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(midpoint.x - pillThickness / 2, midpoint.y - pillLen / 2),
                                        size = Size(pillThickness, pillLen),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillThickness / 2)
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 6. Floating Magnifier Zoom Bubble
            magnifierBitmap?.let { zoomBitmap ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        modifier = Modifier.size(130.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = zoomBitmap.asImageBitmap(),
                                contentDescription = "Zoom",
                                modifier = Modifier
                                    .size(118.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.FillBounds
                            )
                            // Red precision crosshair at the center
                            Canvas(modifier = Modifier.size(118.dp)) {
                                val strokeWidth = 2.dp.toPx()
                                val len = 8.dp.toPx()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                
                                // Vertical line
                                drawLine(
                                    color = Color.Red,
                                    start = Offset(center.x, center.y - len),
                                    end = Offset(center.x, center.y + len),
                                    strokeWidth = strokeWidth
                                )
                                // Horizontal line
                                drawLine(
                                    color = Color.Red,
                                    start = Offset(center.x - len, center.y),
                                    end = Offset(center.x + len, center.y),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Drag hit testing helper function
private fun getDragTarget(
    offset: Offset,
    rect: CropRect,
    threshold: Float
): DragTarget {
    val x = offset.x
    val y = offset.y

    // 1. Corners
    if (dist(x, y, rect.left, rect.top) < threshold) return DragTarget.TOP_LEFT
    if (dist(x, y, rect.right, rect.top) < threshold) return DragTarget.TOP_RIGHT
    if (dist(x, y, rect.left, rect.bottom) < threshold) return DragTarget.BOTTOM_LEFT
    if (dist(x, y, rect.right, rect.bottom) < threshold) return DragTarget.BOTTOM_RIGHT

    // 2. Edges
    if (y >= rect.top - threshold && y <= rect.top + threshold && x >= rect.left && x <= rect.right) return DragTarget.TOP_EDGE
    if (y >= rect.bottom - threshold && y <= rect.bottom + threshold && x >= rect.left && x <= rect.right) return DragTarget.BOTTOM_EDGE
    if (x >= rect.left - threshold && x <= rect.left + threshold && y >= rect.top && y <= rect.bottom) return DragTarget.LEFT_EDGE
    if (x >= rect.right - threshold && x <= rect.right + threshold && y >= rect.top && y <= rect.bottom) return DragTarget.RIGHT_EDGE

    // 3. Body
    if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) return DragTarget.BODY

    return DragTarget.NONE
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}
