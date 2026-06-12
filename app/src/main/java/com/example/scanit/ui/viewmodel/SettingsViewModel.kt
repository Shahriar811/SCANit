package com.example.scanit.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import androidx.datastore.preferences.core.stringPreferencesKey

class SettingsViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val pdfPageSizeKey = stringPreferencesKey("pdf_page_size")
    private val ocrIgnoreNumbersKey = booleanPreferencesKey("ocr_ignore_numbers")

    val isDarkTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[isDarkThemeKey] ?: false
    }

    val pdfPageSize: Flow<String> = dataStore.data.map { preferences ->
        preferences[pdfPageSizeKey] ?: "A4"
    }

    val ocrIgnoreNumbers: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ocrIgnoreNumbersKey] ?: false
    }

    fun setDarkTheme(isDarkTheme: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[isDarkThemeKey] = isDarkTheme
            }
        }
    }

    fun setPdfPageSize(size: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[pdfPageSizeKey] = size
            }
        }
    }

    fun setOcrIgnoreNumbers(ignore: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[ocrIgnoreNumbersKey] = ignore
            }
        }
    }
}
