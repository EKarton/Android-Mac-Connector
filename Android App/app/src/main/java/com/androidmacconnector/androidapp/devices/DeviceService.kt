package com.androidmacconnector.androidapp.devices

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.volley.Request
import com.android.volley.VolleyError
import com.androidmacconnector.androidapp.utils.WebService
import com.androidmacconnector.androidapp.utils.WebServiceResponseHandler
import org.json.JSONException
import org.json.JSONObject
import java.lang.String.format

interface DeviceService {
    fun isDeviceRegistered(authToken: String, androidDeviceId: String, handler: IsDeviceRegisteredHandler)
    fun registerDevice(authToken: String, androidDeviceId: String, capabilities: List<String>, handler: RegisterDeviceHandler)
    fun updateDeviceCapabilities(authToken: String, deviceId: String, capabilities: List<String>, handler: UpdateDeviceCapabilitiesHandler)
}

class DeviceWebService(context: Context): WebService(context), DeviceService {
    companion object {
        private const val LOG_TAG = "DeviceWebService"
        private const val IS_DEVICE_REGISTERED_PATH = "/api/v1/devices/registered"
        private const val REGISTER_DEVICE_PATH = "/api/v1/devices/register"
        private const val UPDATE_DEVICE_CAPABILITIES_PATH = "/api/v1/devices/%s/capabilities"
    }

    override fun isDeviceRegistered(authToken: String, androidDeviceId: String, handler: IsDeviceRegisteredHandler) {
        Log.d(LOG_TAG, "Checking if device is registered")

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(IS_DEVICE_REGISTERED_PATH)
            .appendQueryParameter("device_type", "android")
            .appendQueryParameter("android_device_id", androidDeviceId)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeRequest(Request.Method.GET, uri.toString(), null, headers, handler)
    }

    override fun registerDevice(authToken: String, androidDeviceId: String, capabilities: List<String>, handler: RegisterDeviceHandler) {
        Log.d(LOG_TAG, "Registering device")

        val jsonBody = JSONObject()
        jsonBody.put("device_type", "android")
        jsonBody.put("android_device_id", androidDeviceId)
        jsonBody.put("capabilities", capabilities)

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(REGISTER_DEVICE_PATH)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeRequest(Request.Method.POST, uri.toString(), jsonBody, headers, handler)
    }

    override fun updateDeviceCapabilities(authToken: String, deviceId: String, capabilities: List<String>, handler: UpdateDeviceCapabilitiesHandler) {
        Log.d(LOG_TAG, "Registering device")

        val jsonBody = JSONObject()
        jsonBody.put("capabilities", capabilities)

        val apiPath = UPDATE_DEVICE_CAPABILITIES_PATH.format(deviceId)

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(apiPath)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeRequest(Request.Method.PUT, uri.toString(), jsonBody, headers, handler)
    }
}

abstract class IsDeviceRegisteredHandler : WebServiceResponseHandler {
    companion object {
        private const val LOG_TAG = "IsDevRegHandler"
    }

    override fun onErrorResponse(error: VolleyError) {
        Log.d(LOG_TAG, "Received ERROR Response: " + error.message)
        onError(error)
    }

    override fun onResponse(response: JSONObject?) {
        Log.d(LOG_TAG, "Received HTTP Response: $response")

        if (response != null && response.has("is_registered")) {
            onSuccess(response.getBoolean("is_registered"))
        } else {
            onError(JSONException("Missing is_registered in response:" + response.toString()))
        }
    }

    abstract fun onSuccess(isRegistered: Boolean)
    abstract fun onError(exception: Exception)
}

abstract class RegisterDeviceHandler : WebServiceResponseHandler {
    companion object {
        private const val LOG_TAG = "RegDeviceHandler"
    }

    override fun onErrorResponse(error: VolleyError) {
        Log.d(LOG_TAG, "Received ERROR Response: " + error.message)
        onError(error)
    }

    override fun onResponse(response: JSONObject?) {
        Log.d(LOG_TAG, "Received HTTP Response: $response")

        if (response != null && response.has("device_id")) {
            onSuccess(response.getString("device_id"))
        } else {
            onError(JSONException("Missing device_id in response: " + response.toString()))
        }
    }

    abstract fun onSuccess(deviceId: String)
    abstract fun onError(exception: Exception)
}

abstract class UpdateDeviceCapabilitiesHandler : WebServiceResponseHandler {
    companion object {
        private const val LOG_TAG = "UpdateDeviceCapHandler"
    }

    override fun onErrorResponse(error: VolleyError) {
        Log.d(LOG_TAG, "Received ERROR Response: " + error.message)
        onError(error)
    }

    override fun onResponse(response: JSONObject?) {
        Log.d(LOG_TAG, "Received HTTP Response: $response")
        onSuccess(response)
    }

    abstract fun onSuccess(response: JSONObject?)
    abstract fun onError(exception: Exception)
}