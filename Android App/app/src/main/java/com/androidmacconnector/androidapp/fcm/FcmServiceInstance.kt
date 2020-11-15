package com.androidmacconnector.androidapp.fcm

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.androidmacconnector.androidapp.MainActivity
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.devices.UpdatePushNotificationTokenHandler
import com.androidmacconnector.androidapp.mqtt.MqttService
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FcmServiceInstance : FirebaseMessagingService() {
    private lateinit var deviceService: DeviceWebService

    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    override fun onCreate() {
        super.onCreate()
        this.deviceService = DeviceWebService(this.applicationContext)
    }

    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "Received new FCM token: $token")

        // Get device id
        val deviceId = getDeviceIdSafely(this) ?: return

        // Get the access token
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(false)?.addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result?.token == null) {
                Log.w(LOG_TAG, "Failed to get access token")
                return@addOnCompleteListener
            }

            val accessToken = task.result!!.token!!

            // Upload the fcm token to our server
            deviceService.updatePushNotificationToken(accessToken, deviceId, token, object : UpdatePushNotificationTokenHandler() {
                override fun onSuccess() {}

                override fun onError(exception: Exception) {
                    Log.d(LOG_TAG, "Failed to update token: $exception")
                }
            })
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "Received new message: $remoteMessage")

        // It doesn't do much except for waking up the MQTT Service
        Intent(this, MqttService::class.java).also {
            startService(it)
        }
    }
}