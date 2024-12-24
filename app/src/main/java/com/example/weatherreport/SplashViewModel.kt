package com.example.weatherreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class SplashViewModel : ViewModel() {
    private val _mutableStateFlow = MutableStateFlow(true)
    val isLoading = _mutableStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            imitateLoading()
        }
    }

    private suspend fun imitateLoading() {
        delay(5000)
        _mutableStateFlow.value = false
    }
}