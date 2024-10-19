package com.pentuss.dialerinfo.remote

import com.pentuss.dialerinfo.data.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("get_caller")
    suspend fun getCallerInfo(@Query("caller") callerId: String): ApiResponse
}
