package com.androidmacconnector.androidapp.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.volley.Request
import com.android.volley.VolleyError
import com.androidmacconnector.androidapp.utils.WebService
import com.androidmacconnector.androidapp.utils.WebServiceResponseHandler
import org.json.JSONException
import org.json.JSONObject


interface SmsService {
    fun notifyNewSmsMessageRecieved(
        newSmsMessage: ReceivedSmsMessage,
        handler: NotifyNewSmsMessageReceivedHandler
    )

    fun updateSmsMessageSentStatus(
        uuid: String,
        newStatus: String,
        handler: UpdateSmsSentStatusHandler
    )

    fun updateSmsThreads(
        threads: List<SmsThreadSummary>,
        handler: UpdateSmsThreadsHandler
    )

    fun updateSmsMessagesForThread(
        threadId: String,
        messages: List<MySmsMessage>,
        handler: UpdateSmsMessagesForThreadHandler
    )
}

class SmsWebService(context: Context) : WebService(context), SmsService {

    companion object {
        private const val LOG_TAG = "WebService"

        // Paths
        private const val NOTIFY_RECEIVED_SMS_MESSAGE_PATH = "/api/v1/%s/sms/messages/new"
        private const val UPDATE_SMS_SENT_STATUS_PATH = "/api/v1/%s/sms/messages/%s/status"
        private const val UPDATE_SMS_THREADS_PATH = "/api/v1/%s/sms/threads"
        private const val UPDATE_SMS_MESSAGES_FOR_THREAD_PATH = "/api/v1/%s/sms/threads/%s/messages"
    }

    override fun notifyNewSmsMessageRecieved(newSmsMessage: ReceivedSmsMessage, handler: NotifyNewSmsMessageReceivedHandler) {
        val jsonBody = JSONObject()
        jsonBody.put("address", newSmsMessage.contactInfo)
        jsonBody.put("body", newSmsMessage.data)
        jsonBody.put("timestamp", newSmsMessage.timestamp)

        val apiPath = java.lang.String.format(NOTIFY_RECEIVED_SMS_MESSAGE_PATH, "android")
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .appendEncodedPath(apiPath)
            .build()

        Log.d(LOG_TAG, "Making HTTP request to $uri")

        makeRequest(Request.Method.POST, uri.toString(), jsonBody, null, handler)
    }

    override fun updateSmsMessageSentStatus(uuid: String, newStatus: String, handler: UpdateSmsSentStatusHandler) {
        val jsonBody = JSONObject()
        jsonBody.put("job_status", newStatus)

        val apiPath = java.lang.String.format(UPDATE_SMS_SENT_STATUS_PATH, "<device-id>", uuid)
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .appendEncodedPath(apiPath)
            .build()

        Log.d(LOG_TAG, "Making HTTP request to $uri")

        makeRequest(Request.Method.PUT, uri.toString(), jsonBody, null, handler)
    }

    override fun updateSmsThreads(threads: List<SmsThreadSummary>, handler: UpdateSmsThreadsHandler) {
        val jsonBody = JSONObject()

        val apiPath = java.lang.String.format(UPDATE_SMS_THREADS_PATH, "<device-id>")
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .appendEncodedPath(apiPath)
            .build()

        Log.d(LOG_TAG, "Making HTTP request to $uri")

        makeRequest(Request.Method.PUT, uri.toString(), jsonBody, null, handler)
    }

    override fun updateSmsMessagesForThread(threadId: String, messages: List<MySmsMessage>, handler: UpdateSmsMessagesForThreadHandler) {
        val jsonBody = JSONObject()

        val apiPath = java.lang.String.format(UPDATE_SMS_MESSAGES_FOR_THREAD_PATH, "<device-id>", threadId)
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .appendEncodedPath(apiPath)
            .build()

        makeRequest(Request.Method.PUT, uri.toString(), jsonBody, null, handler)
    }
}

abstract class AndroidMacConnectorServiceBaseHandler : WebServiceResponseHandler {
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

data class ReceivedSmsMessage(
    val contactInfo: String,
    val data: String,
    val timestamp: Int
)

abstract class NotifyNewSmsMessageReceivedHandler : AndroidMacConnectorServiceBaseHandler() {
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