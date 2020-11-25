package com.androidmacconnector.androidapp.notifications

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONObject

/**
 * This class listens to any new notifications, publishes them, and handles
 * responses to their notifications
 */
class NotificationsListener: NotificationListenerService() {

    companion object {
        const val LOG_TAG = "NotificationsListener"
        const val RESPONSE_ACTION = "NotificationResponseAction"
        const val RESPONSE_TOPIC = "notification/responses"
    }

    private lateinit var publisher: NewNotificationsPublisher
    private lateinit var responder: NotificationResponder

    override fun onCreate() {
        super.onCreate()

        this.publisher = NewNotificationsPublisher(this)
        this.responder = NotificationResponder(this)
    }

    /**
     * Called when a new notification is posted
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn != null && isNotificationPublishable(sbn.notification)) {
            this.publisher.publishNotification(sbn)
        }
    }

    /**
     * Called when a notification was removed
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        Log.d(LOG_TAG, "Notification removed: ${sbn?.key}")
    }

    /**
     * Checks if the notification is a notification that we want to publish
     * There are notifications that we don't want to publish, such as notification histories,
     * ongoing events (like downloads), notifications from foreground services, etc.
     */
    private fun isNotificationPublishable(notification: Notification): Boolean {
        // Ignore events that represent foreground services
        if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) {
            Log.d(LOG_TAG, "Is foreground services")
            return false
        }

        // Ignore ongoing events like downloads
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            Log.d(LOG_TAG, "Is ongoing event")
            return false
        }

        if ((notification.flags and Notification.FLAG_LOCAL_ONLY) != 0) {
            Log.d(LOG_TAG, "Is local only")
            return false
        }

        // There is a case where gmail apps send duplicate notifications
        // link: https://stackoverflow.com/questions/45890487/android-onnotificationposted-is-called-twice-for-gmail-and-whatsapp
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            Log.d(LOG_TAG, "Is group summary")
            return false
        }

        // There are cases where new notifications are made on top of one another
        // but Android sends this twice. We ignore this
        if ((notification.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0) {
            Log.d(LOG_TAG, "FLAG_ONLY_ALERT_ONCE")
            return false
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)

        if (intent != null && intent.action == RESPONSE_ACTION) {
            handleNotificationResponse(intent)
        }

        return result
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun handleNotificationResponse(intent: Intent) {
        val payload = intent.getStringExtra("payload")
        Log.d(LOG_TAG, "Payload: ${payload.toString()}")

        val jsonBody = JSONObject(payload.toString())

        val key: String
        if (jsonBody.has("key")) {
            key = jsonBody.getString("key")
        } else {
            Log.d(LOG_TAG, "Cannot get key")
            return
        }

        val actionType: String
        if (jsonBody.has("action_type")) {
            actionType = jsonBody.getString("action_type")
        } else {
            Log.d(LOG_TAG, "Cannot get action type")
            return
        }

        val actionTitle: String
        if (jsonBody.has("action_title")) {
            actionTitle = jsonBody.getString("action_title")

        } else {
            Log.d(LOG_TAG, "Cannot get action title")
            return
        }

        val notifications = getActiveNotifications(arrayOf(key))
        if (notifications.isEmpty()) {
            Log.d(LOG_TAG, "Cannot get notification with key $key")
            return
        }

        val sbn = notifications[0]

        if (actionType == "direct_reply_action" && jsonBody.has("action_reply_message")) {
            val replyMessage = jsonBody.getString("action_reply_message")
            responder.replyWithMessage(sbn, actionTitle, replyMessage)

        } else if (actionType == "action_button") {
            responder.reply(sbn, actionTitle)

        } else {
            throw IllegalArgumentException("Unknown action type: $actionType")
        }
    }
}