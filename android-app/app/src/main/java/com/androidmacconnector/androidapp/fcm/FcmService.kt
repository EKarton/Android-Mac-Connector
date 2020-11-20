package com.androidmacconnector.androidapp.fcm

import android.content.Intent
import android.util.Log
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.androidmacconnector.androidapp.sms.messages.ReadSmsMessagesReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FcmService : FirebaseMessagingService() {
    private lateinit var sessionStore: SessionStoreImpl
    private lateinit var deviceService: DeviceWebServiceImpl
    private lateinit var deviceRegistrationService: DeviceRegistrationService

    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        deviceService = DeviceWebServiceImpl(this)
        deviceRegistrationService = DeviceRegistrationService(this, sessionStore, deviceService)
    }

    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "Received new FCM token: $token")

        deviceRegistrationService.getDeviceId { deviceId, err ->
            if (err != null) {
                Log.d(ReadSmsMessagesReceiver.LOG_TAG, "Error getting device id: $err")
                return@getDeviceId
            }

            if (deviceId.isNullOrBlank()) {
                Log.d(ReadSmsMessagesReceiver.LOG_TAG, "Device is not registered")
                return@getDeviceId
            }

            sessionStore.getAuthToken { authToken, err2 ->
                if (err2 != null || authToken.isBlank()) {
                    Log.w(LOG_TAG, "Error when getting access token: $err2")
                    return@getAuthToken
                }

                // Upload the fcm token to our server
                deviceService.updatePushNotificationToken(authToken, deviceId, token) { err3 ->
                    if (err3 != null) {
                        Log.d(LOG_TAG, "Failed to update token: $err3")
                    }
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