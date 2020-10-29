package com.androidmacconnector.androidapp.sms.messages

import android.content.Context
import android.net.Uri
import com.android.volley.Request
import com.android.volley.VolleyError
import com.androidmacconnector.androidapp.utils.WebService
import com.androidmacconnector.androidapp.utils.WebServiceResponseHandler
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

interface GetSmsMessagesResult {
    fun buildResponse(): JSONObject
}

class GetSmsMessagesSuccessfulResult(private val threads: List<SmsMessage>): GetSmsMessagesResult {
    override fun buildResponse(): JSONObject {
        val jsonArray = JSONArray()
        threads.forEach {
            val jsonObject = JSONObject()
            jsonObject.put("message_id", it.messageId)
            jsonObject.put("address", it.address)
            jsonObject.put("person", it.person)
            jsonObject.put("body", it.body)
            jsonObject.put("is_read", it.readState)
            jsonObject.put("type", it.type)

            jsonArray.put(jsonObject)
        }

        val jsonBody = JSONObject()
        jsonBody.put("status", "success")
        jsonBody.put("results", jsonArray)

        return jsonBody
    }
}

class GetSmsMessagesFailedResult(private val reason: String): GetSmsMessagesResult {
    override fun buildResponse(): JSONObject {
        val jsonBody = JSONObject()
        jsonBody.put("status", "failed")
        jsonBody.put("results", reason)

        return jsonBody
    }
}


interface GetSmsMessegesResultsPublisher {
    fun publishResults(deviceId: String, jobId: String, result: GetSmsMessagesResult, handler: ResponseHandler)
}

class GetSmsMessegesResultsWebPublisher(context: Context): WebService(context), GetSmsMessegesResultsPublisher {
    companion object {
        private const val PUBLISH_GET_SMS_MESSAGES_RESULTS = "api/v1/%s/jobs/%s/results"
    }

    override fun publishResults(deviceId: String, jobId: String, result: GetSmsMessagesResult, handler: ResponseHandler) {
        val jsonBody = result.buildResponse()
        val apiPath = java.lang.String.format(PUBLISH_GET_SMS_MESSAGES_RESULTS, deviceId, jobId)
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .appendEncodedPath(apiPath)
            .build()

        makeJsonObjectRequest(Request.Method.POST, uri.toString(), jsonBody, null, handler)
    }
}

abstract class ResponseHandler : WebServiceResponseHandler {
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