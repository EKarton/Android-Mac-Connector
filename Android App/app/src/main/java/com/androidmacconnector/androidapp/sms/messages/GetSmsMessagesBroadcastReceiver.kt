package com.androidmacconnector.androidapp.sms.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.androidmacconnector.androidapp.mqtt.MqttService
import com.androidmacconnector.androidapp.utils.getDeviceId
import org.json.JSONArray
import org.json.JSONObject

class GetSmsMessagesBroadcastReceiver: BroadcastReceiver() {
    companion object {
        private const val LOG_TAG = "GetSmsMessagesBR"
    }
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
        override fun doWork(): Result {
            val limit = inputData.getInt("limit", 0)
            val start = inputData.getInt("start", 0)
            val threadId = inputData.getString("thread_id") ?: return Result.failure()

            val contentResolver = applicationContext.contentResolver
            val service = GetSmsMessagesServiceImpl()
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
                messageJson.put("phone_number", it.address)
                messageJson.put("person", it.person)
                messageJson.put("body", it.body)
                messageJson.put("read_state", it.readState)
                messageJson.put("time", it.time)
                messageJson.put("type", it.type)

                body.put(messageJson)
            }
            payload.put("messages", body)

            // Submit a job to our MQTT service with details for publishing
            val startIntent = Intent(this.applicationContext, MqttService::class.java)
            startIntent.action = MqttService.PUBLISH_INTENT_ACTION
            startIntent.putExtra("topic", "${getDeviceId(this.applicationContext)}/sms/messages/query-results")
            startIntent.putExtra("payload", payload.toString().toByteArray())

            this.applicationContext.startService(startIntent)
        }
    }
}