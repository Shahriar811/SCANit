package com.example.scanit.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

import com.example.scanit.ui.common.IosBackButton

import com.example.scanit.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiImageTextPreviewScreen(
    imageUris: List<Uri>,
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNext: (String) -> Unit
) {
    var extractedText by remember { mutableStateOf("") }
    var isRecognizing by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val ocrIgnoreNumbers by settingsViewModel.ocrIgnoreNumbers.collectAsState(initial = false)

    LaunchedEffect(imageUris) {
        val stringBuilder = StringBuilder()
        var remaining = imageUris.size
        for (uri in imageUris) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val text = if (ocrIgnoreNumbers) {
                            visionText.text.replace(Regex("[0-9]"), "")
                        } else {
                            visionText.text
                        }
                        stringBuilder.append(text)
                        stringBuilder.append("\n\n")
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        e.printStackTrace()
                    }
                    .addOnCompleteListener {
                        remaining--
                        if (remaining == 0) {
                            extractedText = stringBuilder.toString()
                            isRecognizing = false
                        }
                    }
            } catch (e: IOException) {
                e.printStackTrace()
                remaining--
                if (remaining == 0) {
                    extractedText = stringBuilder.toString()
                    isRecognizing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Edit Text",
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
            if (!isRecognizing) {
                FloatingActionButton(
                    onClick = { onNavigateToNext(extractedText) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Next"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isRecognizing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Extracting text from ${imageUris.size} image${if (imageUris.size > 1) "s" else ""}...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(imageUris) { uri ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Scanned Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { extractedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        label = { 
                            Text(
                                "Extracted Text",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
