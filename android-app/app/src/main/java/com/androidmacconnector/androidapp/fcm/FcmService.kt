package com.androidmacconnector.androidapp.fcm

import android.content.Intent
import android.util.Log
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FcmService : FirebaseMessagingService() {
    private lateinit var sessionStore: SessionStoreImpl
    private lateinit var deviceService: DeviceWebService

    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    override fun onCreate() {
        super.onCreate()
        deviceService = DeviceWebService(this.applicationContext)
        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
    }

    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "Received new FCM token: $token")

        // Get device id
        val deviceId = getDeviceIdSafely(this) ?: return

        sessionStore.getAuthToken { authToken, err ->
            if (err != null) {
                Log.w(LOG_TAG, "Error when getting access token: $err")
                return@getAuthToken
            }

            // Upload the fcm token to our server
            deviceService.updatePushNotificationToken(authToken, deviceId, token) { err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Failed to update token: $err")
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "Received new message: ${remoteMessage.originalPriority} ${remoteMessage.priority}")

        // It doesn't do much except for waking up the MQTT Service
        Intent(this, MQTTService::class.java).also {
            startService(it)
        }
    }
}