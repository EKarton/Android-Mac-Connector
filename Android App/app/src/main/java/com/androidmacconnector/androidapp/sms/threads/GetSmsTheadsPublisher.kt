package com.androidmacconnector.androidapp.sms.threads

import android.content.Context
import android.net.Uri
import com.android.volley.Request
import com.android.volley.VolleyError
import com.androidmacconnector.androidapp.utils.WebService
import com.androidmacconnector.androidapp.utils.WebServiceResponseHandler
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

interface GetSmsThreadsResult {
    fun buildResponse(): JSONObject
}

class GetSmsThreadsSuccessfulResult(private val threads: List<SmsThreadSummary>): GetSmsThreadsResult {
    override fun buildResponse(): JSONObject {
        val jsonArrayBody = JSONArray()
        threads.forEach {
            val jsonObject = JSONObject()
            jsonObject.put("thread_id", it.threadId)
            jsonObject.put("phone_number", it.phoneNumber)
            jsonObject.put("contact_name", it.contactName)
            jsonObject.put("num_unread_messages", it.numUnreadMessages)
            jsonObject.put("num_messages", it.numMessages)
            jsonObject.put("last_time_message_sent", it.lastMessageTime)
            jsonObject.put("last_message_sent", it.lastMessage)

            jsonArrayBody.put(jsonObject)
        }

        val jsonBody = JSONObject()
        jsonBody.put("status", "success")
        jsonBody.put("results", jsonArrayBody)

        return jsonBody
    }
}

class GetSmsThreadFailedResult(private val reason: String): GetSmsThreadsResult {
    override fun buildResponse(): JSONObject {
        val jsonBody = JSONObject()
        jsonBody.put("status", "failed")
        jsonBody.put("results", reason)

        return jsonBody
    }
}

interface GetSmsThreadsResultPublisher {
    fun publishResults(
        deviceId: String,
        jobId: String,
        result: GetSmsThreadsResult,
        handler: PublishResultsHandler
    )
}

class GetSmsThreadsResultWebPublisher(context: Context) : WebService(context), GetSmsThreadsResultPublisher {

    companion object {
        private const val PUBLISH_SMS_THREAD_RESULTS = "api/v1/%s/jobs/%s/results"
    }

    override fun publishResults(deviceId: String, jobId: String, result: GetSmsThreadsResult, handler: PublishResultsHandler) {
        val jsonBody = result.buildResponse()
        val apiPath = java.lang.String.format(PUBLISH_SMS_THREAD_RESULTS, deviceId, jobId)
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .appendEncodedPath(apiPath)
            .build()

        makeJsonObjectRequest(Request.Method.POST, uri.toString(), jsonBody, null, handler)
    }
}

abstract class PublishResultsHandler : WebServiceResponseHandler {
    override fun onErrorResponse(error: VolleyError) {
        onError(error)
    }

    override fun onResponse(response: JSONObject?) {
        try {
            if (response?.getString("status") != "success") {
                onError(Exception("Status returned is not successful!"))
            } else {
                onSuccess()
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    abstract fun onSuccess()
    abstract fun onError(exception: Exception)
}