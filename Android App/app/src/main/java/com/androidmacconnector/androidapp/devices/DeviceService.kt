package com.androidmacconnector.androidapp.devices

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.androidmacconnector.androidapp.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.lang.String.format

/**
 * A class used to represent a device
 */
data class Device(
    val deviceId: String,
    val name: String,
    val type: String,
    val capabilities: List<String>): Serializable {

    fun canPingDevice(): Boolean {
        return capabilities.contains("ping_device")
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

/**
 * An interface for device management
 */
interface DeviceService {
    fun isDeviceRegistered(authToken: String, deviceType: String, hardwareId: String, handler: (Boolean?, Throwable?) -> Unit)
    fun registerDevice(authToken: String, deviceType: String, hardwareId: String, capabilities: List<String>, handler: (String?, Throwable?) -> Unit)
    fun unregisterDevice(authToken: String, deviceId: String, handler: (Throwable?) -> Unit)
    fun getDevices(authToken: String, handler: (List<Device>, Throwable?) -> Unit)
    fun updatePushNotificationToken(authToken: String, deviceId: String, newToken: String, handler: (Throwable?) -> Unit)
}

/**
 * A class used to make REST calls to the server for managing devices
 */
class DeviceWebService(private val context: Context): DeviceService {
    companion object {
        private const val LOG_TAG = "DeviceWebService"
        private const val IS_DEVICE_REGISTERED_PATH = "/api/v1/devices/registered"
        private const val REGISTER_DEVICE_PATH = "/api/v1/devices/register"
        private const val UNREGISTER_DEVICE_PATH = "api/v1/devices/%s"
        private const val GET_DEVICES_PATH = "/api/v1/devices"
        private const val UPDATE_PUSH_NOTIFICATION_TOKEN_PATH = "/api/v1/devices/%s/token"
    }

    private val requestQueue: RequestQueue = VolleyRequestQueue.getInstance(context.applicationContext).requestQueue

    override fun isDeviceRegistered(authToken: String, deviceType: String, hardwareId: String, handler: (Boolean?, Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Checking if device is registered")

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(IS_DEVICE_REGISTERED_PATH)
            .appendQueryParameter("device_type", deviceType)
            .appendQueryParameter("hardware_id", hardwareId)
            .build()
            .toString()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.GET, uri, null, headers) { json, err ->
            if (err != null) {
                handler(null, err)
                return@makeJsonObjectRequest
            }

            if (json != null && json.has("is_registered")) {
                handler(json.getBoolean("is_registered"), null)
                return@makeJsonObjectRequest
            }
            handler(null, IllegalArgumentException("No result for whether the device exists or not"))
        }
    }

    override fun registerDevice(authToken: String, deviceType: String, hardwareId: String, capabilities: List<String>, handler: (String?, Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Registering device")

        val capabilitiesJsonArray = JSONArray()
        capabilities.forEach { capabilitiesJsonArray.put(it) }

        val jsonBody = JSONObject()
            .put("device_type", deviceType)
            .put("hardware_id", hardwareId)
            .put("capabilities", capabilitiesJsonArray)

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(REGISTER_DEVICE_PATH)
            .build()
            .toString()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.POST, uri, jsonBody, headers) { json, err ->
            if (err != null) {
                handler(null, err)
                return@makeJsonObjectRequest
            }

            if (json != null && json.has("device_id")) {
                handler(json.getString("device_id"), null)
                return@makeJsonObjectRequest
            }
            handler(null, null)
        }
    }

    override fun unregisterDevice(authToken: String, deviceId: String, handler: (Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Unregistering device $deviceId")

        val apiPath = UNREGISTER_DEVICE_PATH.format(deviceId)
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(apiPath)
            .build()
            .toString()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.DELETE, uri, null, headers) { json, err ->
            if (err != null) {
                handler(err)
                return@makeJsonObjectRequest
            }

            if (json != null && json.getString("status") == "success") {
                handler(null)
                return@makeJsonObjectRequest
            }

            handler(JSONException("Missing status=success in response:"))
        }
    }

    override fun getDevices(authToken: String, handler: (List<Device>, Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Getting devices")

        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(GET_DEVICES_PATH)
            .build()
            .toString()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.GET, uri, null, headers) { json, err ->
            if (err != null) {
                handler(emptyList(), err)
                return@makeJsonObjectRequest
            }

            if (json == null || !json.has("devices")) {
                handler(emptyList(), JSONException("Missing devices in response"))
                return@makeJsonObjectRequest
            }

            val jsonDevices = json.getJSONArray("devices")
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
    }

    override fun updatePushNotificationToken(authToken: String, deviceId: String, newToken: String, handler: (Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Updating device token")

        val jsonBody = JSONObject().put("new_token", newToken)

        val apiPath = UPDATE_PUSH_NOTIFICATION_TOKEN_PATH.format(deviceId)
        val uri = Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .encodedPath(apiPath)
            .build()
            .toString()

        val headers = mapOf("Authorization" to format("Bearer %s", authToken))
        makeJsonObjectRequest(Request.Method.PUT, uri, jsonBody, headers) { _, err ->
            handler(err)
        }
    }

    private fun getServerProtocol(): String {
        return context.getString(R.string.protocol)
    }

    private fun getServerAuthority(): String {
        return context.getString(R.string.authority)
    }

    private fun makeJsonObjectRequest(method: Int, url: String, jsonBody: JSONObject?, headers: Map<String, String>?, handler: (JSONObject?, Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Making HTTP request to $url with payload $jsonBody and header $headers")

        val errorListener = Response.ErrorListener { error -> handler(null, error) }
        val responseHandler = Response.Listener<JSONObject?> { response -> handler(response, null) }

        val request = object: JsonObjectRequest(method, url, jsonBody, responseHandler, errorListener) {
            override fun getHeaders(): Map<String, String> {
                return headers ?: emptyMap()
            }
        }

        request.retryPolicy = DefaultRetryPolicy(
            500000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(request)
    }
}

/**
 * Queue for devices
 * Refer to https://developer.android.com/training/volley/requestqueue for more info
 */
class VolleyRequestQueue(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VolleyRequestQueue? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleyRequestQueue(context).also {
                    INSTANCE = it
                }
            }
    }

    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
}