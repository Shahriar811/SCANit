package com.example.scanit.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.scanit.util.FileUtil
import com.example.scanit.util.ImageAdjustment
import com.example.scanit.util.ImageProcessor
import com.example.scanit.ui.common.IosBackButton
import java.io.File

data class ScannedImage(
    val uri: Uri,
    val adjustment: ImageAdjustment = ImageAdjustment()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExport: (List<Uri>) -> Unit,
    onNavigateToCrop: (Uri) -> Unit
) {
    val context = LocalContext.current
    var scannedImages by remember { mutableStateOf<List<ScannedImage>>(emptyList()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cropTargetIndex by remember { mutableStateOf<Int?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri?.let { uri ->
                    val savedFile = FileUtil.saveImageFromUri(context, uri)
                    scannedImages = scannedImages + ScannedImage(Uri.fromFile(savedFile))
                    imageUri = null
                }
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val savedFile = FileUtil.saveImageFromUri(context, it)
                scannedImages = scannedImages + ScannedImage(Uri.fromFile(savedFile))
            }
        }
    )

    val multipleGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            uris.forEach { uri ->
                val savedFile = FileUtil.saveImageFromUri(context, uri)
                scannedImages = scannedImages + ScannedImage(Uri.fromFile(savedFile))
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createImageUri(context)
                imageUri = uri
                cameraLauncher.launch(uri)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Document Scanner",
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
            if (scannedImages.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        // Apply adjustments and get final URIs
                        val adjustedUris = scannedImages.mapIndexed { index, scannedImage ->
                            val originalBitmap = ImageProcessor.loadBitmap(context, scannedImage.uri)
                            if (originalBitmap != null && 
                                (scannedImage.adjustment.brightness != 0f || 
                                 scannedImage.adjustment.contrast != 0f || 
                                 scannedImage.adjustment.rotation != 0 ||
                                 scannedImage.adjustment.filter != com.example.scanit.util.ScanFilter.ORIGINAL)) {
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
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Export",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
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
            ) {
                if (scannedImages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )

                        Text(
                            text = "Scan Documents",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Add photos to create a document",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) -> {
                                            val uri = createImageUri(context)
                                            imageUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                        else -> {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Camera")
                            }

                            Button(
                                onClick = { multipleGalleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gallery")
                            }
                        }
                    }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(scannedImages) { index, scannedImage ->
                            ImageAdjustmentCard(
                                scannedImage = scannedImage,
                                onAdjustmentChanged = { adjustment ->
                                    scannedImages = scannedImages.toMutableList().apply {
                                        this[index] = scannedImage.copy(adjustment = adjustment)
                                    }
                                },
                                onDelete = {
                                    scannedImages = scannedImages.filterIndexed { i, _ -> i != index }
                                },
                                onCrop = {
                                    cropTargetIndex = index
                                }
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                onClick = {
                                    multipleGalleryLauncher.launch("image/*")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Add More Photos",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Full-screen crop overlay when cropping an image
            cropTargetIndex?.let { index ->
                ImageCropScreen(
                    imageUri = scannedImages[index].uri,
                    onNavigateBack = { cropTargetIndex = null },
                    onCropCompleted = { croppedUri ->
                        scannedImages = scannedImages.toMutableList().apply {
                            this[index] = this[index].copy(uri = croppedUri)
                        }
                        cropTargetIndex = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageAdjustmentCard(
    scannedImage: ScannedImage,
    onAdjustmentChanged: (ImageAdjustment) -> Unit,
    onDelete: () -> Unit,
    onCrop: () -> Unit
) {
    val context = LocalContext.current
    var brightness by remember { mutableStateOf(scannedImage.adjustment.brightness) }
    var contrast by remember { mutableStateOf(scannedImage.adjustment.contrast) }
    var rotation by remember { mutableStateOf(scannedImage.adjustment.rotation) }
    var filter by remember { mutableStateOf(scannedImage.adjustment.filter) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(scannedImage.uri, brightness, contrast, rotation, filter) {
        val originalBitmap = ImageProcessor.loadBitmap(context, scannedImage.uri)
        if (originalBitmap != null) {
            previewBitmap = ImageProcessor.applyAdjustments(
                originalBitmap,
                brightness,
                contrast,
                rotation,
                filter
            )
            onAdjustmentChanged(ImageAdjustment(brightness, contrast, rotation, filter))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(scannedImage.uri),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
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

            // Preset Filters Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Preset Filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        Pair(com.example.scanit.util.ScanFilter.ORIGINAL, "Original"),
                        Pair(com.example.scanit.util.ScanFilter.AUTO_ENHANCE, "Magic Color"),
                        Pair(com.example.scanit.util.ScanFilter.BLACK_AND_WHITE, "B&W Doc"),
                        Pair(com.example.scanit.util.ScanFilter.GRAYSCALE, "Grayscale")
                    )
                    filters.forEach { (filterVal, name) ->
                        val selected = filter == filterVal
                        FilterChip(
                            selected = selected,
                            onClick = { filter = filterVal },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Rotation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rotation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            rotation = (rotation - 90) % 360
                            if (rotation < 0) rotation += 360
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.RotateLeft,
                            contentDescription = "Rotate Left",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${rotation}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(40.dp)
                    )
                    IconButton(
                        onClick = {
                            rotation = (rotation + 90) % 360
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.RotateRight,
                            contentDescription = "Rotate Right",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Brightness slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brightness",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${brightness.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueRange = -100f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Contrast slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contrast",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${contrast.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = contrast,
                    onValueChange = { contrast = it },
                    valueRange = -100f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
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

