package com.androidmacconnector.androidapp.sms.sender

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.util.Log
import androidx.work.*
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.androidmacconnector.androidapp.sms.messages.ReadSmsMessagesReceiver
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject


/**
 * A receiver used to receive requests for when someone wants to send an sms message through this device
 */
class SendSmsReceiver: BroadcastReceiver() {
    companion object {
        const val LOG_TAG = "SendSmsReceiver"
        const val REQUESTS_TOPIC = "sms/send-message-requests"
        const val RESULTS_TOPIC = "sms/send-message-results"

        fun getRequiredPermissions(): List<String> {
            return listOf(Manifest.permission.SEND_SMS)
        }
    }

    /** This method is called when the BroadcastReceiver is receiving an Intent broadcast. **/
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

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
        private val sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        private val deviceWebService = DeviceWebServiceImpl(context)
        private val deviceRegistrationService = DeviceRegistrationService(context, sessionStore, deviceWebService)

        override fun doWork(): Result {
            val phoneNumber = inputData.getString("phone_number") ?: return Result.failure()
            val message = inputData.getString("message") ?: return Result.failure()
            val messageId = inputData.getString("message_id")
            val publishSmsResults = messageId != null

            this.sendSMS(phoneNumber, message, messageId ?: "", publishSmsResults)
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
                    var status = ""
                    var reason: String? = null

                    when (resultCode) {
                        Activity.RESULT_OK -> {
                            Log.d(LOG_TAG, "SMS sent")
                            status = "success"
                        }
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                            Log.d(LOG_TAG, "Generic failure")
                            status = "failure"
                            reason = "generic-failure"
                        }
                        SmsManager.RESULT_ERROR_NO_SERVICE -> {
                            Log.d(LOG_TAG, "No service")
                            status = "failure"
                            reason = "no-reason"
                        }
                        SmsManager.RESULT_ERROR_NULL_PDU -> {
                            Log.d(LOG_TAG, "Null PDU")
                            status = "failure"
                            reason = "no-reason"
                        }
                        SmsManager.RESULT_ERROR_RADIO_OFF -> {
                            Log.d(LOG_TAG, "Radio off")
                            status = "failure"
                            reason = "radio-off"
                        }
                    }

                    // Ask the MQTT service to publish an event
                    if (publishSmsResults) {
                        publishSendSmsResult(messageId, status, reason)
                    }
                }
            }

            // DELIVERY BroadcastReceiver
            val deliverSMS: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    var status = ""
                    var reason: String? = null
                    when (resultCode) {
                        Activity.RESULT_OK -> {
                            Log.d(LOG_TAG, "SMS delivered")
                            status = "delivered"
                        }
                        Activity.RESULT_CANCELED -> {
                            Log.d(LOG_TAG, "SMS not delivered")
                            status = "failed"
                            reason = "unknown-error"
                        }
                    }

                    if (publishSmsResults) {
                        publishSendSmsResult(messageId, status, reason)
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

        private fun publishSendSmsResult(messageId: String, status: String, reason: String?) {
            Log.d(LOG_TAG, "Publishing send sms results")

            deviceRegistrationService.getDeviceId { deviceId, err ->
                if (err != null) {
                    Log.d(ReadSmsMessagesReceiver.LOG_TAG, "Error getting device id: $err")
                    return@getDeviceId
                }

                if (deviceId.isNullOrBlank()) {
                    Log.d(ReadSmsMessagesReceiver.LOG_TAG, "Device is not registered")
                    return@getDeviceId
                }

                val payload = JSONObject()
                payload.put("message_id", messageId)
                payload.put("status", status)
                payload.put("reason", reason)

                // Submit a job to our MQTT service with details for publishing
                val startIntent = Intent(this.applicationContext, MQTTService::class.java)
                startIntent.action = MQTTService.PUBLISH_INTENT_ACTION
                startIntent.putExtra("topic", "${deviceId}/$RESULTS_TOPIC")
                startIntent.putExtra("payload", payload.toString())

                this.applicationContext.startService(startIntent)
            }
        }
    }
}