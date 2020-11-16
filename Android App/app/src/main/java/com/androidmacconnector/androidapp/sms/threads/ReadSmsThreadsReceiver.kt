package com.androidmacconnector.androidapp.sms.threads

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.androidmacconnector.androidapp.utils.getDeviceId
import org.json.JSONArray
import org.json.JSONObject

/**
 * A receiver used to receive requests to query sms threads on this device
 */
class ReadSmsThreadsReceiver: BroadcastReceiver() {
    companion object {
        const val LOG_TAG = "GetSmsThreadsReceiver"
        const val REQUESTS_TOPIC = "sms/threads/query-requests"
        const val RESULTS_TOPIC = "sms/threads/query-results"


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
            "start" to jsonBody.getInt("start")
        )

        val workRequest = OneTimeWorkRequestBuilder<GetSmsThreadsWorker>()
            .setInputData(myData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    class GetSmsThreadsWorker(context: Context, params: WorkerParameters): Worker(context, params) {
        override fun doWork(): Result {
            val limit = inputData.getInt("limit", 0)
            val start = inputData.getInt("start", 0)

            val contentResolver = applicationContext.contentResolver
            val service = ReadSmsThreadsServiceImpl()
            val threads = service.getSmsThreadsSummary(contentResolver, limit, start)

            this.publishQueryResults(limit, start, threads)
            return Result.success()
        }

        private fun publishQueryResults(limit: Int, start: Int, threads: List<SmsThreadSummary>) {
            Log.d(LOG_TAG, "Publishing sms thread results")

            val payload = JSONObject()
            payload.put("limit", limit)
            payload.put("start", start)

            val body = JSONArray()
            threads.forEach {
                val threadJson = JSONObject()
                threadJson.put("thread_id", it.threadId)
                threadJson.put("phone_number", it.phoneNumber)
                threadJson.put("contact_name", it.contactName)
                threadJson.put("num_unread_messages", it.numUnreadMessages)
                threadJson.put("num_messages", it.numMessages)
                threadJson.put("last_message_sent", it.lastMessage)
                threadJson.put("time_last_message_sent", it.lastMessageTime)

                body.put(threadJson)
            }
            payload.put("threads", body)

            // Submit a job to our MQTT service with details for publishing
            val startIntent = Intent(this.applicationContext, MQTTService::class.java)
            startIntent.action = MQTTService.PUBLISH_INTENT_ACTION
            startIntent.putExtra("topic", "${getDeviceId(this.applicationContext)}/$RESULTS_TOPIC")
            startIntent.putExtra("payload", payload.toString())

            this.applicationContext.startService(startIntent)
        }
    }
}