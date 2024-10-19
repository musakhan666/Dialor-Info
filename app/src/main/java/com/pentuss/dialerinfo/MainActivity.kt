package com.pentuss.dialerinfo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pentuss.dialerinfo.data.ApiResponse
import com.pentuss.dialerinfo.data.Customer
import com.pentuss.dialerinfo.data.Rental
import com.pentuss.dialerinfo.ui.main.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.pentuss.dialerinfo.util.PermissionsViewModel


class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val permissionModel: PermissionsViewModel by viewModels()

    // BroadcastReceiver to handle the received caller ID
    private val callerIdReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val callerId = intent?.getStringExtra("CALLER_ID")
            if (callerId != null) {
                Log.d("MainActivity", "Caller ID received: $callerId")
                // Pass the caller ID to the ViewModel to fetch details
                viewModel.fetchCallerDetails(callerId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission(this)
        }

        val filter = IntentFilter("com.gammaplay.findmyphone.CALLER_ID")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(callerIdReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(callerIdReceiver, filter)
        }

        setContent {
            RentalApp(viewModel = viewModel)
        }

        permissionModel.checkPermissions(this) {
            if (true) permissionModel.requestPermissions(this)
        }
    }
}
private fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    Toast.makeText(context, "Please allow overlay permission", Toast.LENGTH_LONG).show()
    context.startActivity(intent)
}

@Composable
fun RentalApp(viewModel: MainViewModel) {
    val apiResponse = viewModel.apiResponse
    val errorMessage = viewModel.errorMessage

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (errorMessage != null) {
                ErrorCard(message = errorMessage)
            } else if (apiResponse != null) {
                HandleApiResponse(response = apiResponse)
            } else {
                ShowNoDetailsFound()
            }
        }
    }
}

@Composable
fun HandleApiResponse(response: ApiResponse) {
    val customer = response.customer
    val rental = response.rental

    when (customer?.status) {
        -1 -> ShowNoDetailsFound(true)
        0 -> ShowBannedCustomer(customer)
        1 -> {
            if (rental?.status == 0) {
                ShowCustomerInfo(customer)
            } else if (rental?.status == 1) {
                ShowRentalDetails(rental, customer)
            }
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(Color.Red.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = "Error: $message",
            color = Color.White,
            modifier = Modifier.padding(16.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ShowNoDetailsFound(showCallerId: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = if (showCallerId) "No details found for this caller." else "No details found",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun ShowBannedCustomer(customer: Customer) {
    Card(
        colors = CardDefaults.cardColors(Color.Red.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Customer Banned: ${customer.name}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            customer.comment?.let {
                Text(
                    text = "Reason: $it",
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ShowCustomerInfo(customer: Customer) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Customer: ${customer.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "NID: ${customer.nid}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ShowRentalDetails(rental: Rental, customer: Customer) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShowCustomerInfo(customer)

            Text(
                text = "Outstanding: \$${rental.outstanding}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            Text(
                text = "Days Remaining: ${rental.days}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            val isExpired = hasExpired(rental.expiry)
            Text(
                text = "Expiry: ${formatExpiryDate(rental.expiry)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(if (isExpired) Color.Red.copy(alpha = 0.8f) else Color.Transparent)
                    .padding(8.dp),
                color = if (isExpired) Color.White else Color.Black
            )
        }
    }
}

fun hasExpired(expiry: String?): Boolean {
    val now = System.currentTimeMillis()
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    val expiryDate = expiry?.let { dateFormat.parse(it) }
    return expiryDate?.time ?: now < now
}

fun formatExpiryDate(expiry: String?): String {
    if (expiry == null) return "Unknown"

    val now = System.currentTimeMillis()
    val expiryDate = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).parse(expiry)
    expiryDate?.let {
        val diffMs = it.time - now
        val diffDays = diffMs / (1000 * 60 * 60 * 24)
        val diffHours = (diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        val diffMinutes = (diffMs % (1000 * 60 * 60)) / (1000 * 60)

        return "${diffDays} Day(s), ${diffHours} Hour(s), ${diffMinutes} Min(s)"
    }

    return "Unknown"
}
