package com.example.scanit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanit.util.FileUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel : ViewModel() {
    private val _scannedFiles = MutableStateFlow<List<File>>(emptyList())
    val scannedFiles: StateFlow<List<File>> = _scannedFiles

    fun loadScannedFiles(context: Context) {
        viewModelScope.launch {
            _scannedFiles.value = FileUtil.getScannedFiles(context)
        }
    }
}
