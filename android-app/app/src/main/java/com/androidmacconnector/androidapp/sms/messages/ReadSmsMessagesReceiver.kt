package com.androidmacconnector.androidapp.sms.messages

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

class ReadSmsMessagesReceiver: BroadcastReceiver() {
    companion object {
        const val LOG_TAG = "GetSmsMessagesReceiver"
        const val REQUESTS_TOPIC = "sms/messages/query-requests"
        const val RESULTS_TOPIC = "sms/messages/query-results"

        fun getRequiredPermissions(): List<String> {
            return listOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS)
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
            "limit" to jsonBody.getInt("limit"),
            "start" to jsonBody.getInt("start"),
            "thread_id" to jsonBody.getString("thread_id")
        )

        val workRequest = OneTimeWorkRequestBuilder<GetSmsMessagesWorker>()
            .setInputData(myData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    class GetSmsMessagesWorker(context: Context, params: WorkerParameters): Worker(context, params) {
        private val sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        private val deviceWebService = DeviceWebServiceImpl(context)
        private val deviceRegistrationService = DeviceRegistrationService(context, sessionStore, deviceWebService)

        override fun doWork(): Result {
            val limit = inputData.getInt("limit", 0)
            val start = inputData.getInt("start", 0)
            val threadId = inputData.getString("thread_id") ?: return Result.failure()

            val contentResolver = applicationContext.contentResolver
            val service = ReadSmsMessagesServiceImpl()
            val messages = service.getSmsMessagesFromThread(contentResolver, threadId, limit, start)

            this.publishQueryResults(threadId, limit, start, messages)
            return Result.success()
        }

        private fun publishQueryResults(threadId: String, limit: Int, start: Int, messages: List<SmsMessage>) {
            Log.d(LOG_TAG, "Publishing sms message results")

            val payload = JSONObject()
            payload.put("limit", limit)
            payload.put("start", start)
            payload.put("thread_id", threadId)

            val body = JSONArray()
            messages.forEach {
                val messageJson = JSONObject()
                messageJson.put("message_id", it.messageId)
                messageJson.put("phone_number", it.phoneNumber)
                messageJson.put("person", it.person)
                messageJson.put("body", it.body)
                messageJson.put("read_state", it.readState)
                messageJson.put("time", it.time)
                messageJson.put("type", it.type)

                body.put(messageJson)
            }
            payload.put("messages", body)

            Log.d(LOG_TAG, payload.toString())

            deviceRegistrationService.getDeviceId { deviceId, err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Error getting device id: $err")
                    return@getDeviceId
                }

                if (deviceId.isNullOrBlank()) {
                    Log.d(LOG_TAG, "Device is not registered")
                    return@getDeviceId
                }

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