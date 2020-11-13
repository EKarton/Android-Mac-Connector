package com.androidmacconnector.androidapp.sms.receiver

import android.Manifest
import android.annotation.TargetApi
import android.content.*
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import com.androidmacconnector.androidapp.mqtt.MqttService
import com.androidmacconnector.androidapp.mqtt.MqttService.Companion.PUBLISH_INTENT_ACTION
import com.androidmacconnector.androidapp.utils.getDeviceId
import org.json.JSONObject

/**
 * This class is responsible for receiving SMS messages from Android
 */
class ReceivedSmsBroadcastReceiver : BroadcastReceiver() {
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

                val payload = JSONObject()
                payload.put("phone_number", smsMessage.displayOriginatingAddress)
                payload.put("body", smsMessage.messageBody)
                payload.put("timestamp", (smsMessage.timestampMillis / 1000).toInt())

                // Submit a job to our MQTT service with details for publishing
                val startIntent = Intent(context, MqttService::class.java)
                startIntent.action = PUBLISH_INTENT_ACTION
                startIntent.putExtra("topic", "${getDeviceId(context)}/sms/received-messages")
                startIntent.putExtra("payload", payload.toString().toByteArray())

                context.startService(startIntent)
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
