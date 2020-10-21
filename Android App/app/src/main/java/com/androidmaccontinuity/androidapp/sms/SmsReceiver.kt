package com.androidmaccontinuity.androidapp.sms

import android.Manifest
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log

/**
 * This class is responsible for receiving SMS messages from Android
 */
abstract class SmsBroadcastReceiver : BroadcastReceiver() {
    private val TAG: String = SmsBroadcastReceiver::class.java.simpleName

    abstract fun onReceiveSmsMessages(smsMessages: List<SmsMessage>)

    @TargetApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        // Get the SMS message.
        val bundle = intent.extras
        val format = bundle!!.getString("format")

        // Retrieve the SMS message received.
        val pdus = bundle["pdus"] as Array<*>?

        val smsMessages = arrayListOf<SmsMessage>()

        if (pdus != null) {
            for (i in pdus.indices) {
                val smsMessage = getSmsMessageFromPdu(pdus[i] as ByteArray, format)
                smsMessages.add(smsMessage)

                Log.d(TAG, "Received SMS message: $smsMessage")
            }
        }

        this.onReceiveSmsMessages(smsMessages)
    }

    private fun getSmsMessageFromPdu(pdu: ByteArray, format: String?): SmsMessage {
        // If Android version M or newer, use the appropriate createFromPdu function
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return SmsMessage.createFromPdu(pdu, format)
        } else {
            return SmsMessage.createFromPdu(pdu)
        }
    }
}

/**
 * A class used to send sms messages to its corresponding devices
 */
class SmsReceiverService : SmsBroadcastReceiver() {
    fun getRequiredPermissions(): List<String> {
        return arrayListOf(Manifest.permission.RECEIVE_SMS)
    }

    override fun onReceiveSmsMessages(smsMessages: List<SmsMessage>) {
        // Get the contact info for each sms message
        smsMessages.forEach {
            val phoneNumber = it.displayOriginatingAddress
            val body = it.messageBody
            val timestamp = (it.timestampMillis / 1000).toInt()
        }
    }
}

data class ReceivedSmsMessage(
    val contactInfo: String,
    val data: String,
    val timestamp: Int
)
