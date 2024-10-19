package com.pentuss.dialerinfo.ui.main

import com.pentuss.dialerinfo.data.ApiResponse
import com.pentuss.dialerinfo.remote.RetrofitClient

class MainRepository {

    // Function to fetch caller details from the API
    suspend fun fetchCallerDetails(callerId: String): ApiResponse {
        return RetrofitClient.apiService.getCallerInfo(callerId)
    }
}
