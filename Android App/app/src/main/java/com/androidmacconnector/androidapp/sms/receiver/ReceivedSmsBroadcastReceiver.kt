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
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.EventBusBuilder

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

        if (pdus != null) {
            for (i in pdus.indices) {
                val smsMessage = getSmsMessageFromPdu(pdus[i] as ByteArray, format)

                Log.d(LOG_TAG, "Received SMS message: $smsMessage")
                EventBus.getDefault().post(smsMessage)
            }
        }
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
