package com.example.scanit.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.scanit.ui.common.ScanButton
import com.example.scanit.ui.common.IosBackButton
import com.example.scanit.util.ImageProcessor
import com.example.scanit.util.ImageAdjustment
import com.example.scanit.util.ScanFilter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ContinuousScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExport: (List<Uri>) -> Unit,
    onNavigateToCrop: (Uri) -> Unit
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val capturedImages = remember { mutableStateListOf<ScannedImage>() }
    var tempImageUri: Uri? = null
    var cropTargetIndex by remember { mutableStateOf<Int?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempImageUri?.let { uri ->
                    val savedFile = com.example.scanit.util.FileUtil.saveImageFromUri(context, uri)
                    capturedImages.add(ScannedImage(Uri.fromFile(savedFile)))
                }
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            uris.forEach { uri ->
                val savedFile = com.example.scanit.util.FileUtil.saveImageFromUri(context, uri)
                capturedImages.add(ScannedImage(Uri.fromFile(savedFile)))
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createImageUri(context)
                tempImageUri = uri
                cameraLauncher.launch(uri)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Text Scanner",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IosBackButton(onClick = onNavigateBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (capturedImages.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        // Apply all filters and adjustments, then save temporary files for OCR
                        val adjustedUris = capturedImages.map { scannedImage ->
                            val originalBitmap = ImageProcessor.loadBitmap(context, scannedImage.uri)
                            if (originalBitmap != null && 
                                (scannedImage.adjustment.filter != ScanFilter.ORIGINAL ||
                                 scannedImage.adjustment.rotation != 0)) {
                                val adjustedBitmap = ImageProcessor.applyAdjustments(
                                    originalBitmap,
                                    scannedImage.adjustment.brightness,
                                    scannedImage.adjustment.contrast,
                                    scannedImage.adjustment.rotation,
                                    scannedImage.adjustment.filter
                                )
                                ImageProcessor.saveAdjustedBitmap(context, adjustedBitmap, scannedImage.uri)
                            } else {
                                scannedImage.uri
                            }
                        }
                        onNavigateToExport(adjustedUris)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Export")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScanButton(
                    text = "Take Photo",
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) -> {
                                val uri = createImageUri(context)
                                tempImageUri = uri
                                cameraLauncher.launch(uri)
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    icon = Icons.Default.PhotoCamera,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ScanButton(
                    text = "Select from Gallery",
                    onClick = { galleryLauncher.launch("image/*") },
                    icon = Icons.Default.Image,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            
                if (capturedImages.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected Images",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${capturedImages.size} image${if (capturedImages.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            count = capturedImages.size,
                            key = { index -> capturedImages[index].uri.hashCode() }
                        ) { index ->
                            val scannedImage = capturedImages[index]
                            ContinuousImageItem(
                                scannedImage = scannedImage,
                                onFilterChanged = { newFilter ->
                                    capturedImages[index] = scannedImage.copy(
                                        adjustment = scannedImage.adjustment.copy(filter = newFilter)
                                    )
                                },
                                onRotate = {
                                    val currentRotation = scannedImage.adjustment.rotation
                                    val nextRotation = (currentRotation + 90) % 360
                                    capturedImages[index] = scannedImage.copy(
                                        adjustment = scannedImage.adjustment.copy(rotation = nextRotation)
                                    )
                                },
                                onCrop = { cropTargetIndex = index },
                                onDelete = { capturedImages.removeAt(index) }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No images selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start by capturing or selecting images",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            // Full-screen crop overlay when cropping an image
            cropTargetIndex?.let { index ->
                ImageCropScreen(
                    imageUri = capturedImages[index].uri,
                    onNavigateBack = { cropTargetIndex = null },
                    onCropCompleted = { croppedUri ->
                        capturedImages[index] = capturedImages[index].copy(uri = croppedUri)
                        cropTargetIndex = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinuousImageItem(
    scannedImage: ScannedImage,
    onFilterChanged: (ScanFilter) -> Unit,
    onRotate: () -> Unit,
    onCrop: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(scannedImage.uri, scannedImage.adjustment.filter, scannedImage.adjustment.rotation) {
        val originalBitmap = ImageProcessor.loadBitmap(context, scannedImage.uri)
        if (originalBitmap != null) {
            previewBitmap = ImageProcessor.applyAdjustments(
                originalBitmap,
                scannedImage.adjustment.brightness,
                scannedImage.adjustment.contrast,
                scannedImage.adjustment.rotation,
                scannedImage.adjustment.filter
            )
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(scannedImage.uri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Top overlay action buttons (Crop, Rotate, Delete)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onCrop,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Crop,
                            contentDescription = "Crop",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = onRotate,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.RotateRight,
                            contentDescription = "Rotate",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Filter selection chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OCR Filter:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                )
                val filters = listOf(
                    Pair(ScanFilter.ORIGINAL, "Original"),
                    Pair(ScanFilter.AUTO_ENHANCE, "Magic Color"),
                    Pair(ScanFilter.BLACK_AND_WHITE, "B&W Doc"),
                    Pair(ScanFilter.GRAYSCALE, "Grayscale")
                )
                filters.forEach { (filterVal, name) ->
                    val selected = scannedImage.adjustment.filter == filterVal
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterChanged(filterVal) },
                        label = { Text(name, style = MaterialTheme.typography.bodySmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }
    }
}

private fun createImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    file.createNewFile()
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}