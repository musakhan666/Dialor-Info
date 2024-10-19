package com.pentuss.dialerinfo.data

data class Rental(
    val status: Int ?= null,
    val outstanding: Int? = null,
    val days: Int? = null,
    val expiry: String? = null,
    val cycle: String? = null
)

data class Customer(
    val name: String? = null,
    val nid: String? = null,
    val comment: String? = null,
    val status: Int? = null
)

data class ApiResponse(
    val rental: Rental? = null,
    val customer: Customer? = null
)
