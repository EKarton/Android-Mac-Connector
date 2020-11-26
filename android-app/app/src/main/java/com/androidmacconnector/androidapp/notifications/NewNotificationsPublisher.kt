package com.androidmacconnector.androidapp.notifications

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

/**
 * This class is responsible for publishing new notifications to the MQTT server
 * so that other devices subscribed can receive the new notification
 */
class NewNotificationsPublisher(private var context: Context) {

    companion object {
        const val LOG_TAG = "NewNotificationsPub"
        const val TOPIC = "notification/new"
    }

    private val sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
    private val deviceWebService = DeviceWebServiceImpl(context)
    private val deviceRegistrationService = DeviceRegistrationService(context, sessionStore, deviceWebService)

    /**
     * Publishes the notification to the MQTT server
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun publishNotification(sbn: StatusBarNotification) {
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
            startIntent.putExtra("topic", "$deviceId/$TOPIC")
            startIntent.putExtra("payload", createPayload(sbn).toString())

            this.context.startService(startIntent)
        }
    }

    /**
     * Creates the payload used to send to the MQTT server
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    private fun createPayload(sbn: StatusBarNotification): JSONObject {
        val payload = JSONObject()
            .put("id", sbn.key)
            .put("app", this.getAppName(sbn))
            .put("time_posted", sbn.notification?.`when`)
            .put("title", this.getTitle(sbn))
            .put("text", this.getContentText(sbn))

        // Inject the actions
        val actionsJson = JSONArray()
        payload.put("actions", actionsJson)

        for (action in getActionButtons(sbn)) {
            val actionJson = JSONObject()
                .put("type", "action_button")
                .put("text", action)
            actionsJson.put(actionJson)
        }

        for (action in getDirectReplyActions(sbn)) {
            val actionJson = JSONObject()
                .put("type", "direct_reply_action")
                .put("text", action)
            actionsJson.put(actionJson)
        }

        return payload
    }

    /**
     * Gets the app name that this notification came from
     */
    private fun getAppName(sbn: StatusBarNotification): String? {
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, PackageManager.GET_META_DATA)
            return packageManager.getApplicationLabel(appInfo).toString()

        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }

    /**
     * Obtains the title from the notification
     */
    private fun getTitle(sbn: StatusBarNotification): String? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras

        // The content title is from the bundle too
        // https://developer.android.com/reference/android/app/Notification#EXTRA_TITLE
        return extras.getString(NotificationCompat.EXTRA_TITLE)
    }

    /**
     * Gets the text from the notification
     */
    private fun getContentText(sbn: StatusBarNotification): String? {
        val notification = sbn.notification ?: return null

        val extras = notification.extras

        // The context text is from the bundle
        // https://developer.android.com/reference/android/app/Notification#EXTRA_TEXT
        if (extras.containsKey(NotificationCompat.EXTRA_BIG_TEXT)) {
            return extras.getString(NotificationCompat.EXTRA_BIG_TEXT)
        }

        if (extras.containsKey(NotificationCompat.EXTRA_TEXT)) {
            return extras.getString(NotificationCompat.EXTRA_TEXT)
        }

        return null
    }

    /**
     * Get the non-reply actions (those that are only buttons and don't take in text)
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    private fun getActionButtons(sbn: StatusBarNotification): List<String> {
        if (sbn.notification.actions == null) {
            return emptyList()
        }

        return sbn.notification.actions
            .filter { it.remoteInputs == null || it.remoteInputs.isEmpty() }
            .mapNotNull { it.title }
            .map { it.toString() }
    }

    /**
     * Get the "reply" actions
     * link: https://developer.android.com/training/notify-user/build-notification#reply-action
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    private fun getDirectReplyActions(sbn: StatusBarNotification): List<String> {
        if (sbn.notification.actions == null) {
            return emptyList()
        }

        return sbn.notification.actions
            .filter {it.remoteInputs != null && it.remoteInputs.isNotEmpty() }
            .mapNotNull { it.title }
            .map { it.toString() }
    }
}