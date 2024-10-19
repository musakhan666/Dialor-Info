package com.pentuss.dialerinfo.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

class CallReceiver : BroadcastReceiver() {

    private var lastState: String? = null  // To track the call state
    private val handler = Handler(Looper.getMainLooper())  // To delay the service stop
    private val delayMillis: Long = 10_000  // 10 seconds delay (in milliseconds)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            when (intent.action) {
                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                    if (state != null) {
                        // If call ends or becomes idle, stop the overlay service after 10 seconds
                        if (state == TelephonyManager.EXTRA_STATE_IDLE || state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                            Log.d("CallReceiver", "Call ended or idle, scheduling overlay stop")
                            handler.postDelayed({
                                context?.stopService(Intent(context, OverlayService::class.java))
                                Log.d("CallReceiver", "Overlay service stopped after 10 seconds")
                            }, delayMillis)
                        }

                        // Handle incoming call ringing
                        if (state == TelephonyManager.EXTRA_STATE_RINGING && incomingNumber != null) {
                            Log.d("CallReceiver", "Incoming call from: $incomingNumber")
                            val formattedNumber = stripCountryCode(context, incomingNumber)

                            // Start the overlay service with the caller ID
                            showPopupOverlay(context, formattedNumber)
                        }
                    }
                    lastState = state
                }

                Intent.ACTION_NEW_OUTGOING_CALL -> {
                    val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)

                    if (outgoingNumber != null && !isUSSD(outgoingNumber)) {  // Avoid USSD
                        val formattedNumber = stripCountryCode(context, outgoingNumber)
                        Log.d("CallReceiver", "Outgoing call to: $formattedNumber")

                        // Start the overlay service with the outgoing number
                        formattedNumber?.let { showPopupOverlay(context, it) }
                    }
                }
            }
        }
    }

    private fun stripCountryCode(context: Context?, phoneNumber: String): String {
        val phoneUtil = PhoneNumberUtil.getInstance()
        return try {
            val regionCode = context?.resources?.configuration?.locale?.country ?: "US"
            val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, regionCode)
            if (phoneUtil.isValidNumber(numberProto)) {
                numberProto.nationalNumber.toString()
            } else {
                phoneNumber
            }
        } catch (e: Exception) {
            Log.e("CallReceiver", "Error parsing phone number: ${e.message}")
            phoneNumber
        }
    }

    private fun showPopupOverlay(context: Context?, phoneNumber: String) {
        // Start the OverlayService and pass the caller ID
        val intent = Intent(context, OverlayService::class.java)
        intent.putExtra("CALLER_ID", phoneNumber)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(intent)
        } else {
            context?.startService(intent)
        }
    }

    private fun isUSSD(phoneNumber: String): Boolean {
        // USSD codes usually start with * and end with #
        return phoneNumber.startsWith("*") && phoneNumber.endsWith("#")
    }
}
