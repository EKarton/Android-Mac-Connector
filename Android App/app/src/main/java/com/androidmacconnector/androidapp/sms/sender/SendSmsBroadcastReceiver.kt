package com.androidmacconnector.androidapp.sms.sender

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.util.Log
import androidx.work.*
import com.androidmacconnector.androidapp.mqtt.MqttClientListener.Companion.SEND_SMS_REQUEST_INTENT
import com.androidmacconnector.androidapp.mqtt.MqttService
import com.androidmacconnector.androidapp.utils.getDeviceId
import org.json.JSONObject


/**
 * A receiver used to receive requests for when someone wants to send an sms message through this device
 */
class SendSmsRequestBroadcastReceiver: BroadcastReceiver() {
    companion object {
        private const val LOG_TAG = "SendSmsBcReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SEND_SMS_REQUEST_INTENT) {
            Log.d(LOG_TAG, "Received send sms intent: $intent")
            val payload = intent.getStringExtra("payload")
            val jsonBody = JSONObject(payload)

            val myData: Data = workDataOf(
                "phone_number" to jsonBody.getString("phone_number"),
                "message" to jsonBody.getString("message")
            )

            val workRequest = OneTimeWorkRequestBuilder<SendSmsWorker>()
                .setInputData(myData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    private class SendSmsWorker(context: Context, params: WorkerParameters): Worker(context, params) {

        override fun doWork(): Result {
            val phoneNumber = inputData.getString("phone_number") ?: return Result.failure()
            val message = inputData.getString("message") ?: return Result.failure()

            // Message id is an uint16 (https://stackoverflow.com/questions/11115364/mqtt-messageid-practical-implementation#:~:text=Message%20id%20is%20an%20unsigned%20int16%20so%20the%20max%20value%20is%2065535.)
            // So if the message id returned is a max int32, then that means that the id was not specified
            val messageId = inputData.getInt("message_id", Int.MAX_VALUE)

            var publishSmsResults = true
            if (kotlin.math.abs(messageId - Int.MAX_VALUE) < 0.00001) {
                publishSmsResults = false
            }

            sendSMS(phoneNumber, message, messageId, publishSmsResults)
            return Result.success()
        }

        private fun sendSMS(phoneNumber: String, message: String, messageId: Int, publishSmsResults: Boolean) {
            // Intent Filter Tags for SMS SEND and DELIVER
            val sentIntentTag = "SMS_SENT"
            val deliveredIntentTag = "SMS_DELIVERED"

            // SEND PendingIntent
            val sentPI = PendingIntent.getBroadcast(this.applicationContext, 0, Intent(sentIntentTag), 0)

            // DELIVER PendingIntent
            val deliveredPI = PendingIntent.getBroadcast(this.applicationContext, 0, Intent(deliveredIntentTag), 0)

            // SEND BroadcastReceiver
            val sendSMS: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (resultCode) {
                        Activity.RESULT_OK -> Log.d(LOG_TAG, "SMS sent")
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Log.d(LOG_TAG, "Generic failure")
                        SmsManager.RESULT_ERROR_NO_SERVICE -> Log.d(LOG_TAG, "No service")
                        SmsManager.RESULT_ERROR_NULL_PDU -> Log.d(LOG_TAG, "Null PDU")
                        SmsManager.RESULT_ERROR_RADIO_OFF -> Log.d(LOG_TAG, "Radio off")
                    }

                    // Ask the MQTT service to publish an event
                    if (publishSmsResults) {
                        publishSendSmsRequestResult(messageId, resultCode)
                    }
                }
            }

            // DELIVERY BroadcastReceiver
            val deliverSMS: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (resultCode) {
                        Activity.RESULT_OK -> Log.d(LOG_TAG, "SMS delivered")
                        Activity.RESULT_CANCELED -> Log.d(LOG_TAG, "SMS not delivered")
                    }

                    if (publishSmsResults) {
                        publishSendSmsRequestResult(messageId, resultCode)
                    }
                }
            }

            // Notify when the SMS has been sent
            this.applicationContext.registerReceiver(sendSMS, IntentFilter(sentIntentTag))

            // Notify when the SMS has been delivered
            this.applicationContext.registerReceiver(deliverSMS, IntentFilter(deliveredIntentTag))

            // Send message
            val sms = SmsManager.getDefault()
            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI)
        }

        private fun publishSendSmsRequestResult(messageId: Int, resultCode: Int) {
            val payload = JSONObject()
            payload.put("message_id", messageId)
            payload.put("result_code", resultCode)

            // Submit a job to our MQTT service with details for publishing
            val startIntent = Intent(this.applicationContext, MqttService::class.java)
            startIntent.action = MqttService.PUBLISH_INTENT_ACTION
            startIntent.putExtra("topic", "${getDeviceId(this.applicationContext)}/send-sms-results")
            startIntent.putExtra("payload", payload.toString().toByteArray())

            this.applicationContext.startService(startIntent)
        }
    }
}