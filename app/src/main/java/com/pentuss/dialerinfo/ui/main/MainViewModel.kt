package com.pentuss.dialerinfo.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pentuss.dialerinfo.data.ApiResponse
import com.pentuss.dialerinfo.remote.RetrofitClient
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    var apiResponse by mutableStateOf<ApiResponse?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    fun fetchCallerDetails(callerId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getCallerInfo(callerId)
                apiResponse = response
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }

}
