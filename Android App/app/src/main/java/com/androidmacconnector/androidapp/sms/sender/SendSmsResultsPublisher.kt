package com.androidmacconnector.androidapp.sms.sender

import android.content.Context
import android.net.Uri
import com.android.volley.Request
import com.android.volley.VolleyError
import com.androidmacconnector.androidapp.utils.WebService
import com.androidmacconnector.androidapp.utils.WebServiceResponseHandler
import org.json.JSONException
import org.json.JSONObject

interface SendSmsResult {
    fun buildResponse(): JSONObject
}

class SendSmsSuccessfulResults(): SendSmsResult {
    override fun buildResponse(): JSONObject {
        val jsonBody = JSONObject()
        jsonBody.put("status", "success")

        return jsonBody
    }
}

class SendSmsFailedResults(private val reason: String): SendSmsResult {
    override fun buildResponse(): JSONObject {
        val jsonBody = JSONObject()
        jsonBody.put("status", "failed")
        jsonBody.put("results", reason)

        return jsonBody
    }
}

interface SendSmsResultsPublisher {
    fun publishResults(deviceId: String, jobId: String, result: SendSmsResult, handler: PublishResultsHandler)
}

class SendSmsResultsWebPublisher(context: Context) : WebService(context), SendSmsResultsPublisher {
    companion object {
        private const val PUBLISH_SEND_SMS_RESULTS = "api/v1/%s/jobs/%s/results"
    }

    override fun publishResults(deviceId: String, jobId: String, result: SendSmsResult, handler: PublishResultsHandler) {
        val jsonBody = result.buildResponse()
        val apiPath = java.lang.String.format(PUBLISH_SEND_SMS_RESULTS, deviceId, jobId)
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