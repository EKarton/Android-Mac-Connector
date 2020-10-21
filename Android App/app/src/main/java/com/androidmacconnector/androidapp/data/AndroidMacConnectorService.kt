package com.androidmacconnector.androidapp.data

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.sms.MySmsMessage
import com.androidmacconnector.androidapp.sms.ReceivedSmsMessage
import com.androidmacconnector.androidapp.sms.SmsThread
import com.androidmacconnector.androidapp.sms.SmsThreadSummary
import org.json.JSONException
import org.json.JSONObject
import java.net.URI


interface AndroidMacConnectorService {
    fun notifyReceivedSmsMessage(
        newSmsMessage: ReceivedSmsMessage,
        handler: NotifyReceivedSmsMessageHandler
    )

    fun updateSmsMessageSentStatus(
        uuid: String,
        newStatus: String,
        handler: UpdateSmsSentStatusHandler
    )

    fun updateSmsThreads(threads: List<SmsThreadSummary>, handler: UpdateSmsThreadsHandler)
    fun updateSmsMessagesForThread(
        threadId: String,
        messages: List<MySmsMessage>,
        handler: UpdateSmsMessagesForThreadHandler
    )
}

class AndroidMacConnectorServiceImpl(private val context: Context) : AndroidMacConnectorService {

    companion object {
        private const val LOG_TAG = "WebService"

        // Paths
        private const val NOTIFY_RECEIVED_SMS_MESSAGE_PATH = "/api/v1/%s/sms/messages/new"
        private const val UPDATE_SMS_SENT_STATUS_PATH = "/api/v1/%s/sms/messages/%s/status"
        private const val UPDATE_SMS_THREADS_PATH = "/api/v1/%s/sms/threads"
        private const val UPDATE_SMS_MESSAGES_FOR_THREAD_PATH = "/api/v1/%s/sms/threads/%s/messages"
    }

    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    override fun notifyReceivedSmsMessage(
        newSmsMessage: ReceivedSmsMessage,
        handler: NotifyReceivedSmsMessageHandler
    ) {
        val jsonBody = JSONObject()
        jsonBody.put("address", newSmsMessage.contactInfo)
        jsonBody.put("body", newSmsMessage.data)
        jsonBody.put("timestamp", newSmsMessage.timestamp)

        val apiEndpoint = java.lang.String.format(NOTIFY_RECEIVED_SMS_MESSAGE_PATH, "<device-id>")

        val uri = URI(
            getServerProtocol(),
            null,
            getServerHostname(),
            getServerPort(),
            apiEndpoint,
            null,
            null
        )

        Log.d(LOG_TAG, "Making HTTP request to $uri")

        val request =
            JsonObjectRequest(Request.Method.POST, uri.toString(), jsonBody, handler, handler)
        request.retryPolicy = DefaultRetryPolicy(
            500000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(request)
    }

    override fun updateSmsMessageSentStatus(
        uuid: String,
        newStatus: String,
        handler: UpdateSmsSentStatusHandler
    ) {
        val jsonBody = JSONObject()
        jsonBody.put("status", newStatus)

        val apiEndpoint = java.lang.String.format(UPDATE_SMS_SENT_STATUS_PATH, "<device-id>", uuid)

        val uri = URI(
            getServerProtocol(),
            null,
            getServerHostname(),
            getServerPort(),
            apiEndpoint,
            null,
            null
        )

        Log.d(LOG_TAG, "Making HTTP request to $uri")

        val request =
            JsonObjectRequest(Request.Method.PUT, uri.toString(), jsonBody, handler, handler)
        request.retryPolicy = DefaultRetryPolicy(
            500000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(request)
    }

    override fun updateSmsThreads(
        threads: List<SmsThreadSummary>,
        handler: UpdateSmsThreadsHandler
    ) {
        val jsonBody = JSONObject()

        val apiEndpoint = java.lang.String.format(UPDATE_SMS_THREADS_PATH, "<device-id>")
        val uri = URI(
            getServerProtocol(),
            null,
            getServerHostname(),
            getServerPort(),
            apiEndpoint,
            null,
            null
        )

        Log.d(LOG_TAG, "Making HTTP request to $uri")

        val request =
            JsonObjectRequest(Request.Method.PUT, uri.toString(), jsonBody, handler, handler)
        request.retryPolicy = DefaultRetryPolicy(
            500000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(request)
    }

    override fun updateSmsMessagesForThread(
        threadId: String,
        messages: List<MySmsMessage>,
        handler: UpdateSmsMessagesForThreadHandler
    ) {
        val jsonBody = JSONObject()

        val apiEndpoint =
            java.lang.String.format(UPDATE_SMS_MESSAGES_FOR_THREAD_PATH, "<device-id>", threadId)
        val uri = URI(
            getServerProtocol(),
            null,
            getServerHostname(),
            getServerPort(),
            apiEndpoint,
            null,
            null
        )

        Log.d(LOG_TAG, "Making HTTP request to $uri")

        val request =
            JsonObjectRequest(Request.Method.PUT, uri.toString(), jsonBody, handler, handler)
        request.retryPolicy = DefaultRetryPolicy(
            500000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(request)
    }

    private fun getServerProtocol(): String {
        return context.getString(R.string.protocol)
    }

    private fun getServerHostname(): String {
        return context.getString(R.string.hostname)
    }

    private fun getServerPort(): Int {
        return context.getString(R.string.port).toInt()
    }
}

abstract class AndroidMacConnectorServiceBaseHandler : Response.Listener<JSONObject?>,
    Response.ErrorListener {
    abstract fun getLogTag(): String

    override fun onErrorResponse(error: VolleyError) {
        Log.d(this.getLogTag(), "Received ERROR Response: " + error.message)
        onError(error)
    }

    override fun onResponse(response: JSONObject?) {
        Log.d(this.getLogTag(), "Received HTTP Response: $response")
        try {
            if (response?.getString("status") != "success") {
                onError(Exception("Status returned is not successful!"))
            } else {
                onSuccess(response)
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    abstract fun onSuccess(response: JSONObject)
    abstract fun onError(exception: Exception?)
}

abstract class NotifyReceivedSmsMessageHandler : AndroidMacConnectorServiceBaseHandler() {
    override fun getLogTag(): String {
        return "NewSmsHandler"
    }
}

abstract class UpdateSmsSentStatusHandler : AndroidMacConnectorServiceBaseHandler() {
    override fun getLogTag(): String {
        return "UpdateSmsSentHandler"
    }
}

abstract class UpdateSmsThreadsHandler : AndroidMacConnectorServiceBaseHandler() {
    override fun getLogTag(): String {
        return "UpdateSmsThreadsHandler"
    }
}

abstract class UpdateSmsMessagesForThreadHandler : AndroidMacConnectorServiceBaseHandler() {
    override fun getLogTag(): String {
        return "UpdateSmsMsgThreadsHandler"
    }
}