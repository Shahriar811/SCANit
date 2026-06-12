package com.example.scanit.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.scanit.util.FileUtil
import com.example.scanit.ui.common.ScanButton // Import the common ScanButton
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onScanCompleted: (Uri) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri?.let { uri ->
                    val savedFile = FileUtil.saveImageFromUri(context, uri)
                    onScanCompleted(Uri.fromFile(savedFile))
                }
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val savedFile = FileUtil.saveImageFromUri(context, it)
                onScanCompleted(Uri.fromFile(savedFile))
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
            } else {
                // Handle permission denial
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Scan Document",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
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
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Icon illustration
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "Scan your document",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Capture from camera or select from gallery",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Action buttons
                    ScanButton(
                        text = "Take Photo",
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
                        icon = Icons.Default.PhotoCamera,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    ScanButton(
                        text = "Select from Gallery",
                        onClick = { galleryLauncher.launch("image/*") },
                        icon = Icons.Default.Image
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun createImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    file.createNewFile() // Create the file
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}
