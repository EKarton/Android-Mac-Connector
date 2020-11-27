package com.androidmacconnector.androidapp.devices

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.auth.SessionStore

class DeviceRegistrationService(
    private val context: Context,
    private val sessionStore: SessionStore,
    private val deviceWebService: DeviceWebService) {

    companion object {
        private const val LOG_TAG = "DeviceRegService"
    }

    /**
     * Registers a device
     * It will use the hardware ID of the android device to register the device
     * It will throw an error when it is already registered
     */
    fun registerDevice(capabilities: List<String>, handler: (Throwable?) -> Unit) {
        sessionStore.getAuthToken { authToken, err ->
            if (err != null) {
                Log.d(LOG_TAG, "Error getting auth token: $err")
                handler(err)
                return@getAuthToken
            }

            val deviceType = getDeviceType()
            val hardwareId = getHardwareId()
            val name = "My Android Phone"
            deviceWebService.registerDevice(authToken, deviceType, hardwareId, name, capabilities) webService@ { deviceId, err2 ->
                if (err2 != null) {
                    Log.d(LOG_TAG, "Failed to register device, $err2")
                    handler(err2)
                    return@webService
                }

                if (deviceId.isNullOrBlank()) {
                    Log.d(LOG_TAG, "Device ID is blank")
                    handler(IllegalStateException("Device ID received is blank"))
                    return@webService
                }

                saveDeviceIdToDisk(deviceId)
                handler(null)
            }
        }
    }

    /**
     * It will return the device id first from the cache, and then from the server
     * If it is not registered, it will throw a blank string
     */
    fun getDeviceId(handler: (String?, Throwable?) -> Unit) {
        // Check if it is in the cache and if so, return it
        val cachedDeviceId = getDeviceIdFromDisk()
        if (!cachedDeviceId.isNullOrBlank()) {
            handler(cachedDeviceId, null)
            return
        }

        // Get the device id from the web service, giving it its hardware id
        sessionStore.getAuthToken { authToken, err ->
            if (err != null || authToken.isBlank()) {
                Log.d(LOG_TAG, "Error getting auth token: $err")
                handler(null, err)
                return@getAuthToken
            }

            val deviceType = getDeviceType()
            val hardwareId = getHardwareId()
            deviceWebService.isDeviceRegistered(authToken, deviceType, hardwareId) { isRegistered, deviceId, err2 ->
                if (err2 != null) {
                    Log.d(LOG_TAG, "Error occured when getting device id from server: $err2")
                    handler(null, err2)
                    return@isDeviceRegistered
                }

                if (!isRegistered) {
                    Log.d(LOG_TAG, "Device is not registered")
                    handler(null, null)
                    return@isDeviceRegistered
                }

                if (deviceId.isBlank()) {
                    Log.d(LOG_TAG, "Device ID is blank")
                    handler(null, null)
                    return@isDeviceRegistered
                }

                saveDeviceIdToDisk(deviceId)
                handler(deviceId, null)
            }
        }
    }

    /**
     * It will unregister the device
     * If the device id is missing from cache, it will fetch it from the server
     */
    fun unregisterDevice(handler: (Throwable?) -> Unit) {
        sessionStore.getAuthToken { authToken, err1 ->
            if (err1 != null) {
                Log.d(LOG_TAG, "Error getting auth token: $err1")
                handler(err1)
                return@getAuthToken
            }

            val deviceId = getDeviceIdFromDisk()
            if (deviceId.isNullOrBlank()) {
                this.getDeviceId { fetchedDeviceId, err2 ->
                    if (err2 != null) {
                        Log.d(LOG_TAG, "Error fetching device id during unregistration")
                        handler(err2)
                        return@getDeviceId
                    }
                    if (fetchedDeviceId.isNullOrBlank()) {
                        Log.d(LOG_TAG, "Device is not registered")
                        handler(err2)
                        return@getDeviceId
                    }
                    unregisterDeviceFromWebService(authToken, fetchedDeviceId, handler)
                    return@getDeviceId
                }
                return@getAuthToken
            }

            unregisterDeviceFromWebService(authToken, deviceId, handler)
        }
    }

    private fun unregisterDeviceFromWebService(authToken: String, deviceId: String, handler: (Throwable?) -> Unit) {
        deviceWebService.unregisterDevice(authToken, deviceId) webService@ { err3 ->
            if (err3 != null) {
                Log.d(LOG_TAG, "Error unregistering device: $err3")
                handler(err3)
                return@webService
            }

            Log.d(LOG_TAG, "Successfully unregistered device")

            removeDeviceIdFromDisk()
            handler(null)
        }
    }

    private fun getHardwareId(): String{
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun getDeviceType(): String {
        return "android_phone"
    }

    private fun saveDeviceIdToDisk(deviceId: String) {
        val fileName = context.getString(R.string.app_data_file_key)
        val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val key = context.getString(R.string.android_device_id)

        with (sharedPreferences.edit()) {
            putString(key, deviceId)
            apply()
        }
    }

    private fun getDeviceIdFromDisk(): String? {
        val fileName = context.getString(R.string.app_data_file_key)
        val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val key = context.getString(R.string.android_device_id)

        if (sharedPreferences.contains(key)) {
            return sharedPreferences.getString(key, null)
        }
        return null
    }

    private fun removeDeviceIdFromDisk() {
        val fileName = context.getString(R.string.app_data_file_key)
        val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val key = context.getString(R.string.android_device_id)

        with (sharedPreferences.edit()) {
            remove(key)
            apply()
        }
    }
}