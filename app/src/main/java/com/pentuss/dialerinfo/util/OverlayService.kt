package com.pentuss.dialerinfo.util
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.pentuss.dialerinfo.MainActivity
import com.pentuss.dialerinfo.R
import com.pentuss.dialerinfo.data.ApiResponse
import com.pentuss.dialerinfo.ui.main.MainRepository
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private val repository = MainRepository()

    // Track whether the overlay view has been added
    private var isOverlayAdded = false

    // Create a coroutine scope for the service
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "overlay_channel"
    }

    override fun onCreate() {
        super.onCreate()

        // Create the notification channel (for Android 8.0+)
        createNotificationChannel()

        // Start the service as a foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callerId = intent?.getStringExtra("CALLER_ID") ?: "Unknown Caller"

        // Fetch caller details using the repository directly
        serviceScope.launch {
            try {
                val response = repository.fetchCallerDetails(callerId)
                withContext(Dispatchers.Main) {
                    // Update the UI on the main thread and show overlay if data is available
                    updateOverlayUI(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateOverlayUI(null)  // Handle the error case by passing null
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isOverlayAdded && ::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        serviceScope.cancel()  // Cancel the coroutine scope when the service is destroyed
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Create a notification for the foreground service
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Service")
            .setContentText("Displaying call information")
            .setSmallIcon(R.drawable.logo)
            .setContentIntent(pendingIntent)
            .build()
    }

    // Create a notification channel for Android 8.0+
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // Function to update the overlay UI based on API response
    private fun updateOverlayUI(response: ApiResponse?) {
        if (response == null) {
            // If no data is available, do not show the overlay
            stopSelf()
            return
        }

        // Inflate the XML layout for the overlay only if it hasn't been added yet
        if (!isOverlayAdded) {
            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

            // Set layout parameters for the overlay
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            // Add the overlay to the WindowManager
            windowManager.addView(overlayView, params)
            isOverlayAdded = true
        }

        val titleTextView: TextView = overlayView.findViewById(R.id.caller_info_title)
        val subtitleTextView: TextView = overlayView.findViewById(R.id.caller_info_subtitle)
        val closeButton: Button = overlayView.findViewById(R.id.close_button)

        val customer = response.customer
        val rental = response.rental

        when (customer?.status) {
            -1 -> {
                titleTextView.text = "No details found"
                subtitleTextView.text = ""
            }
            0 -> {
                // Banned customer with red background
                titleTextView.text = "Customer Banned: ${customer.name}"
                subtitleTextView.text = customer.comment ?: "No additional comments"
                overlayView.setBackgroundResource(R.drawable.gradient_warning)  // Red background
                titleTextView.setTextColor(getColor(R.color.white))  // White text for better contrast
                subtitleTextView.setTextColor(getColor(R.color.white))  // White text for better contrast
            }
            1 -> {
                titleTextView.text = "Customer: ${customer.name} (NID: ${customer.nid})"

                if (rental?.status == 0) {
                    subtitleTextView.text = "No active rental"
                    // Apply the gradient for valid customers (light blue background)
                    overlayView.setBackgroundResource(R.drawable.gradient_valid)
                } else if (rental?.status == 1) {
                    // Calculate outstanding only if above 0
                    if ((rental.outstanding ?: 0) > 0) {
                        val outstandingText = "Outstanding: \$${rental.outstanding}"
                        subtitleTextView.text = outstandingText

                        // Display outstanding as a noticeable button-like box
                        val outstandingButton: Button = overlayView.findViewById(R.id.outstanding_button)
                        outstandingButton.text = outstandingText
                        outstandingButton.visibility = View.VISIBLE  // Show the button
                    } else {
                        subtitleTextView.text = ""
                        val outstandingButton: Button = overlayView.findViewById(R.id.outstanding_button)
                        outstandingButton.visibility = View.GONE  // Hide the button if no outstanding
                    }

                    // Rename to "Days Rented" and format time without 0 days
                    val daysRentedText = if ((rental.days ?: 0) > 0) {
                        "Days Rented: ${rental.days}"
                    } else {
                        ""
                    }

                    val expiryText = formatExpiryTimeWithoutDays(rental.expiry)

                    subtitleTextView.text = "${subtitleTextView.text}\n$daysRentedText\nExpiry: $expiryText"

                    if (hasExpired(rental.expiry)) {
                        // Apply the gradient for expired rentals (red background)
                        overlayView.setBackgroundResource(R.drawable.gradient_warning)
                        titleTextView.setTextColor(getColor(R.color.white))  // White text for better contrast
                        subtitleTextView.setTextColor(getColor(R.color.white))  // White text for better contrast
                    } else {
                        // Apply the gradient for valid rentals (light blue background)
                        overlayView.setBackgroundResource(R.drawable.gradient_valid)
                        titleTextView.setTextColor(getColor(R.color.black))  // Black text for valid customers
                        subtitleTextView.setTextColor(getColor(R.color.black))  // Black text for valid customers
                    }
                }
            }
        }


        closeButton.setOnClickListener {
            stopSelf()  // Stop the service and close the overlay
        }
    }


    private fun hasExpired(expiry: String?): Boolean {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val expiryDate = expiry?.let { dateFormat.parse(it) }
        return expiryDate?.time ?: 0 < System.currentTimeMillis()
    }

    private fun formatExpiryTimeWithoutDays(expiry: String?): String {
        if (expiry == null) return "Unknown"

        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val expiryDate = dateFormat.parse(expiry) ?: return "Unknown"
        val now = System.currentTimeMillis()

        val diffMs = expiryDate.time - now
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60

        return if (diffDays > 0) {
            "$diffDays Day(s) $diffHours Hour(s) $diffMinutes Min(s)"
        } else {
            "$diffHours Hour(s) $diffMinutes Min(s)"
        }
    }

}
