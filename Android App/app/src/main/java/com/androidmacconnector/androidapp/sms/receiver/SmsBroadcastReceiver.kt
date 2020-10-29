package com.androidmacconnector.androidapp.sms.receiver

import android.Manifest
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import com.androidmacconnector.androidapp.utils.getDeviceId

/**
 * This class is responsible for receiving SMS messages from Android
 */
class SmsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val LOG_TAG = "SmsBroadcastReceiver"

        fun getRequiredPermissions(): List<String> {
            return arrayListOf(Manifest.permission.RECEIVE_SMS)
        }
    }

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

                Log.d(LOG_TAG, "Received SMS message: $smsMessage")
            }
        }

        this.onReceiveSmsMessages(context, smsMessages)
    }

    private fun getSmsMessageFromPdu(pdu: ByteArray, format: String?): SmsMessage {
        // If Android version M or newer, use the appropriate createFromPdu function
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return SmsMessage.createFromPdu(pdu, format)
        } else {
            return SmsMessage.createFromPdu(pdu)
        }
    }

    private fun onReceiveSmsMessages(context: Context, smsMessages: List<SmsMessage>) {
        val webService = ReceiveSmsWebNotifier(context)
        val deviceId = getDeviceId(context)

        // Get the contact info for each sms message
        smsMessages.forEach {
            val phoneNumber = it.displayOriginatingAddress
            val body = it.messageBody
            val timestamp = (it.timestampMillis / 1000).toInt()
            val sms = ReceivedSmsMessage(phoneNumber, body, timestamp)

            webService.publishResult(deviceId, sms, object : ResponseHandler() {
                override fun onSuccess() {}
                override fun onError(exception: Exception?) {}
            })
        }
    }
}
