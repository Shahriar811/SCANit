package com.example.scanit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.net.Uri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.scanit.ui.ContinuousScanScreen
import com.example.scanit.ui.CreditsScreen
import com.example.scanit.ui.DocumentExportScreen
import com.example.scanit.ui.DocumentScanScreen
import com.example.scanit.ui.ExportScreen
import com.example.scanit.ui.HomeScreen
import com.example.scanit.ui.ImageCropScreen
import com.example.scanit.ui.ScanScreen
import com.example.scanit.ui.SettingsScreen
import com.example.scanit.ui.TextPreviewScreen
import com.example.scanit.ui.theme.SCANitTheme
import com.example.scanit.ui.viewmodel.SettingsViewModel
import com.example.scanit.ui.MultiImageTextPreviewScreen
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(dataStore)
            )
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState(initial = false)
            SCANitTheme(darkTheme = isDarkTheme) {
                SCANitApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun SCANitApp(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    NavHost(
        navController = navController, 
        startDestination = "home",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(350))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(350))
        }
    ) {
        composable("home") {
            HomeScreen(
                onSettingsClicked = { navController.navigate("settings") },
                onCreditsClicked = { navController.navigate("credits") },
                onContinuousScanClicked = { navController.navigate("continuous_scan") },
                onDocumentScanClicked = { navController.navigate("document_scan") },
                newScanUri = null
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("credits") {
            CreditsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("continuous_scan") {
            ContinuousScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExport = { uris ->
                    val uriStrings = uris.joinToString(separator = ",") { Uri.encode(it.toString()) }
                    navController.navigate("text_preview_multiple/$uriStrings")
                },
                onNavigateToCrop = { uri ->
                    navController.navigate("crop/${Uri.encode(uri.toString())}")
                }
            )
        }
        composable("text_preview/{imageUri}") { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")?.let { Uri.parse(it) }
            if (imageUri != null) {
                TextPreviewScreen(
                    imageUri = imageUri,
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNext = { text ->
                        navController.navigate("export/${Uri.encode(text)}")
                    }
                )
            }
        }
        composable("text_preview_multiple/{imageUris}") { backStackEntry ->
            val imageUris = backStackEntry.arguments?.getString("imageUris")?.split(",")?.map { Uri.parse(it) }
            if (imageUris != null) {
                MultiImageTextPreviewScreen(
                    imageUris = imageUris,
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNext = { text ->
                        navController.navigate("export/${Uri.encode(text)}")
                    }
                )
            }
        }
        composable("export/{text}") { backStackEntry ->
            val text = backStackEntry.arguments?.getString("text")?.let { Uri.decode(it) }
            if (text != null) {
                ExportScreen(
                    text = text,
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
        }
        composable("document_scan") {
            DocumentScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExport = { uris ->
                    val uriStrings = uris.joinToString(separator = ",") { Uri.encode(it.toString()) }
                    navController.navigate("document_export/$uriStrings")
                },
                onNavigateToCrop = { uri ->
                    navController.navigate("crop/${Uri.encode(uri.toString())}")
                }
            )
        }
        composable("crop/{imageUri}") { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")?.let { Uri.parse(it) }
            if (imageUri != null) {
                ImageCropScreen(
                    imageUri = imageUri,
                    onNavigateBack = { navController.popBackStack() },
                    onCropCompleted = { croppedUri ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("croppedUri", croppedUri.toString())
                        navController.popBackStack()
                    }
                )
            }
        }
        composable("document_export/{imageUris}") { backStackEntry ->
            val imageUris = backStackEntry.arguments?.getString("imageUris")?.split(",")?.map { Uri.parse(it) }
            if (imageUris != null) {
                DocumentExportScreen(
                    imageUris = imageUris,
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
