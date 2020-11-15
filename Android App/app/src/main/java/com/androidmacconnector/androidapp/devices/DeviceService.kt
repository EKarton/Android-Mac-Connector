package com.androidmacconnector.androidapp.devices

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.volley.Request
import com.android.volley.VolleyError
import com.androidmacconnector.androidapp.utils.WebService
import com.androidmacconnector.androidapp.utils.WebServiceResponseHandler
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.lang.String.format

interface DeviceService {
    fun isDeviceRegistered(authToken: String, androidDeviceId: String, handler: IsDeviceRegisteredHandler)
    fun registerDevice(authToken: String, androidDeviceId: String, capabilities: List<String>, handler: RegisterDeviceHandler)
    fun getDevices(authToken: String, handler: (List<Device>, Throwable?) -> Unit)
    fun updateDeviceCapabilities(authToken: String, deviceId: String, capabilities: List<String>, handler: UpdateDeviceCapabilitiesHandler)
    fun updatePushNotificationToken(authToken: String, deviceId: String, newToken: String, handler: UpdatePushNotificationTokenHandler)
    fun updatePushNotificationToken2(authToken: String, deviceId: String, newToken: String, handler: (Throwable?) -> Unit)
}

class DeviceWebService(context: Context): WebService(context), DeviceService {
    companion object {
        private const val LOG_TAG = "DeviceWebService"
        private const val IS_DEVICE_REGISTERED_PATH = "/api/v1/devices/registered"
        private const val REGISTER_DEVICE_PATH = "/api/v1/devices/register"
        private const val GET_DEVICES_PATH = "/api/v1/devices"
        private const val UPDATE_DEVICE_CAPABILITIES_PATH = "/api/v1/devices/%s/capabilities"
        private const val UPDATE_PUSH_NOTIFICATION_TOKEN_PATH = "/api/v1/devices/%s/token"
    }

    override fun isDeviceRegistered(authToken: String, androidDeviceId: String, handler: IsDeviceRegisteredHandler) {
        Log.d(LOG_TAG, "Checking if device is registered")

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(IS_DEVICE_REGISTERED_PATH)
            .appendQueryParameter("device_type", "android")
            .appendQueryParameter("hardware_id", androidDeviceId)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.GET, uri.toString(), null, headers, handler)
    }

    override fun registerDevice(authToken: String, androidDeviceId: String, capabilities: List<String>, handler: RegisterDeviceHandler) {
        Log.d(LOG_TAG, "Registering device")

        val capabilitiesJsonArray = JSONArray()
        capabilities.forEach { capabilitiesJsonArray.put(it) }

        val jsonBody = JSONObject()
        jsonBody.put("device_type", "android")
        jsonBody.put("hardware_id", androidDeviceId)
        jsonBody.put("capabilities", capabilitiesJsonArray)

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(REGISTER_DEVICE_PATH)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.POST, uri.toString(), jsonBody, headers, handler)
    }

    override fun getDevices(authToken: String, handler: (List<Device>, Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Getting devices")

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(GET_DEVICES_PATH)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        val jsonHandler = object: WebServiceResponseHandler {
            override fun onResponse(response: JSONObject?) {
                if (response == null || !response.has("devices")) {
                    handler(emptyList(), JSONException("Missing devices in response:"))
                    return
                }

                val jsonDevices = response.getJSONArray("devices")
                val devices = arrayListOf<Device>()
                for (i in 0 until jsonDevices.length()) {
                    val jsonDevice = jsonDevices.getJSONObject(i)

                    // Parse capabilities
                    val capabilities = arrayListOf<String>()
                    val jsonCapabilities = jsonDevice.getJSONArray("capabilities")
                    for (j in 0 until jsonCapabilities.length()) {
                        capabilities.add(jsonCapabilities.getString(j))
                    }

                    // Parse device
                    val device = Device(
                        deviceId=jsonDevice.getString("id"),
                        name=jsonDevice.getString("name"),
                        type=jsonDevice.getString("type"),
                        capabilities=capabilities
                    )
                    devices.add(device)
                }

                handler(devices, null)
            }

            override fun onErrorResponse(error: VolleyError?) {
                handler(emptyList(), error)
            }
        }
        makeJsonObjectRequest(Request.Method.GET, uri.toString(), null, headers, jsonHandler)
    }

    override fun updateDeviceCapabilities(authToken: String, deviceId: String, capabilities: List<String>, handler: UpdateDeviceCapabilitiesHandler) {
        Log.d(LOG_TAG, "Registering device")

        val capabilitiesJsonArray = JSONArray()
        capabilities.forEach { capabilitiesJsonArray.put(it) }

        val jsonBody = JSONObject()
        jsonBody.put("capabilities", capabilitiesJsonArray)

        val apiPath = UPDATE_DEVICE_CAPABILITIES_PATH.format(deviceId)

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(apiPath)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.PUT, uri.toString(), jsonBody, headers, handler)
    }

    override fun updatePushNotificationToken(authToken: String, deviceId: String, newToken: String, handler: UpdatePushNotificationTokenHandler) {
        Log.d(LOG_TAG, "Updating device token")

        val jsonBody = JSONObject()
        jsonBody.put("new_token", newToken)

        val apiPath = UPDATE_PUSH_NOTIFICATION_TOKEN_PATH.format(deviceId)

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(apiPath)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.PUT, uri.toString(), jsonBody, headers, handler)
    }

    override fun updatePushNotificationToken2(authToken: String, deviceId: String, newToken: String, handler: (Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Updating device token")

        val jsonBody = JSONObject()
        jsonBody.put("new_token", newToken)

        val apiPath = UPDATE_PUSH_NOTIFICATION_TOKEN_PATH.format(deviceId)

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(apiPath)
            .build()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest2(Request.Method.PUT, uri.toString(), jsonBody, headers) { res, err ->
            handler(err)
        }
    }
}

class Device(
    val deviceId: String,
    val name: String,
    val type: String,
    val capabilities: List<String>
): Serializable {

    companion object {
        fun createDevicesList(numDevices: Int) : ArrayList<Device> {
            val contacts = ArrayList<Device>()
            for (i in 1..numDevices) {
                contacts.add(Device("Device$i", "My Device", "Mac", listOf("read_sms")))
            }
            return contacts
        }
    }

    fun canPingDevice(): Boolean {
        return capabilities.contains("ping_device")
    }

    fun canReceiveFiles(): Boolean {
        return capabilities.contains("receive_files")
    }

    fun canSendSms(): Boolean {
        return capabilities.contains("send_sms")
    }

    fun canReadSms(): Boolean {
        return capabilities.contains("read_sms")
    }

    fun canReceiveSms(): Boolean {
        return capabilities.contains("receive_sms")
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

abstract class UpdatePushNotificationTokenHandler : WebServiceResponseHandler {
    companion object {
        private const val LOG_TAG = "UpdateFcmTokenHandler"
    }

    override fun onErrorResponse(error: VolleyError) {
        Log.d(LOG_TAG, "Received ERROR Response: " + error.message)
        onError(error)
    }

    override fun onResponse(response: JSONObject?) {
        Log.d(LOG_TAG, "Received HTTP Response: $response")
        onSuccess()
    }

    abstract fun onSuccess()
    abstract fun onError(exception: Exception)
}