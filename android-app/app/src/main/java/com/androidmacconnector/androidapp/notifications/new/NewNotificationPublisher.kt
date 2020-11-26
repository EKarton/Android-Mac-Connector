package com.androidmacconnector.androidapp.notifications.new

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

class NewNotificationPublisher(private val context: Context): NewNotificationHandler() {

    companion object {
        const val LOG_TAG = "NewNotificationsPub"
        const val TOPIC = "notification/new"
    }

    private val sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
    private val deviceWebService = DeviceWebServiceImpl(context)
    private val deviceRegistrationService = DeviceRegistrationService(context, sessionStore, deviceWebService)

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun onHandleNotification(notification: NewNotification): Boolean {
        deviceRegistrationService.getDeviceId { deviceId, err ->
            if (err != null) {
                Log.d(LOG_TAG, "Error getting device id: $err")
                return@getDeviceId
            }

            if (deviceId.isNullOrBlank()) {
                Log.d(LOG_TAG, "Device is not registered")
                return@getDeviceId
            }

            Log.d(LOG_TAG, "Publishing new notification")

            // Submit a job to our MQTT service with details for publishing
            val startIntent = Intent(this.context, MQTTService::class.java)
            startIntent.action = MQTTService.PUBLISH_INTENT_ACTION
            startIntent.putExtra("topic", "$deviceId/${TOPIC}")
            startIntent.putExtra("payload", createPayload(notification).toString())

            this.context.startService(startIntent)
        }

        return true
    }

    /**
     * Creates the payload used to send to the MQTT server
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    private fun createPayload(notification: NewNotification): JSONObject {
        val payload = JSONObject()
            .put("id", notification.getKey())
            .put("app", notification.getAppName(context))
            .put("time_posted", notification.getTimePosted())
            .put("title", notification.getTitle())
            .put("text", notification.getContentText())

        // Inject the actions
        val actionsJson = JSONArray()
        payload.put("actions", actionsJson)

        for (action in notification.getActions()) {
            val actionJson = JSONObject()
                .put("type", action.type)
                .put("text", action.text)
            actionsJson.put(actionJson)
        }

        return payload
    }
}