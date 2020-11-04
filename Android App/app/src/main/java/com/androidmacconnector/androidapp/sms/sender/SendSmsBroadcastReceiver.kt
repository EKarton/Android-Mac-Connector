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
import com.androidmacconnector.androidapp.mqtt.MqttService
import com.androidmacconnector.androidapp.utils.getDeviceId
import org.json.JSONObject


/**
 * A receiver used to receive requests for when someone wants to send an sms message through this device
 */
class SendSmsBroadcastReceiver: BroadcastReceiver() {
    companion object {
        private const val LOG_TAG = "SendSmsBcReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(LOG_TAG, "Received send sms intent: $intent")
        val payload = intent.getStringExtra("payload")
        Log.d(LOG_TAG, "Payload: ${payload.toString()}")

        val jsonBody = JSONObject(payload.toString())

        val myData: Data = workDataOf(
            "phone_number" to jsonBody.getString("phone_number"),
            "message" to jsonBody.getString("message"),
            "message_id" to jsonBody.getString("message_id")
        )

        val workRequest = OneTimeWorkRequestBuilder<SendSmsWorker>()
            .setInputData(myData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    class SendSmsWorker(context: Context, params: WorkerParameters): Worker(context, params) {

        override fun doWork(): Result {
            val phoneNumber = inputData.getString("phone_number") ?: return Result.failure()
            val message = inputData.getString("message") ?: return Result.failure()
            val messageId = inputData.getString("message_id")
            val publishSmsResults = messageId != null

            sendSMS(phoneNumber, message, messageId ?: "", publishSmsResults)
            return Result.success()
        }

        private fun sendSMS(phoneNumber: String, message: String, messageId: String, publishSmsResults: Boolean) {
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
                        publishSendSmsResult(messageId, resultCode)
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
                        publishSendSmsResult(messageId, resultCode)
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

        private fun publishSendSmsResult(messageId: String, resultCode: Int) {
            Log.d(LOG_TAG, "Publishing send sms results")

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