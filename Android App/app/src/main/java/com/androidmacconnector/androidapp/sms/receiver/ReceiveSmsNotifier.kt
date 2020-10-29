package com.androidmacconnector.androidapp.sms.receiver

import android.content.Context
import android.net.Uri
import com.android.volley.Request
import com.android.volley.VolleyError
import com.androidmacconnector.androidapp.utils.WebService
import com.androidmacconnector.androidapp.utils.WebServiceResponseHandler
import org.json.JSONException
import org.json.JSONObject


interface ReceiveSmsNotifier {
    fun publishResult(
        deviceId: String,
        newSmsMessage: ReceivedSmsMessage,
        handler: ResponseHandler
    )
}

class ReceiveSmsWebNotifier(context: Context) : WebService(context), ReceiveSmsNotifier {
    companion object {
        private const val NOTIFY_RECEIVED_SMS_MESSAGE_PATH = "/api/v1/%s/sms/messages/new"
    }

    override fun publishResult(deviceId: String, newSmsMessage: ReceivedSmsMessage, handler: ResponseHandler) {
        val jsonBody = JSONObject()
        jsonBody.put("address", newSmsMessage.contactInfo)
        jsonBody.put("body", newSmsMessage.data)
        jsonBody.put("timestamp", newSmsMessage.timestamp)

        val apiPath = java.lang.String.format(NOTIFY_RECEIVED_SMS_MESSAGE_PATH, deviceId)
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .appendEncodedPath(apiPath)
            .build()

        makeJsonObjectRequest(Request.Method.POST, uri.toString(), jsonBody, null, handler)
    }
}

data class ReceivedSmsMessage(
    val contactInfo: String,
    val data: String,
    val timestamp: Int
)

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
    abstract fun onError(exception: Exception?)
}