package com.androidmacconnector.androidapp.notifications

import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import com.androidmacconnector.androidapp.notifications.new.*
import com.androidmacconnector.androidapp.notifications.response.NotificationResponseActionButtonHandler
import com.androidmacconnector.androidapp.notifications.response.NotificationResponseDirectReplyHandler
import com.androidmacconnector.androidapp.notifications.response.NotificationResponse
import org.json.JSONObject
import java.lang.Exception

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

    private lateinit var incomingHandler: NewNotificationHandler
    private lateinit var actionHandler: NotificationResponseActionButtonHandler
    private lateinit var directReplyHandler: NotificationResponseDirectReplyHandler

    override fun onCreate() {
        super.onCreate()

        this.actionHandler = NotificationResponseActionButtonHandler(this)
        this.directReplyHandler = NotificationResponseDirectReplyHandler(this)

        this.incomingHandler = NewNotificationInvalidFlagsRemover()
        val handler2 = NewNotificationDuplicateRemover()
        val handler3 = NewNotificationPublisher(this)

        this.incomingHandler.setNext(handler2)
        handler2.setNext(handler3)
    }

    /**
     * Called when a new notification is posted
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        try {
            sbn?.run { incomingHandler.handleNotification(NewNotification(sbn)) }

        } catch (e: Exception) {
            Log.w(LOG_TAG, "Exception caught: $e")
            e.printStackTrace()
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
        try {
            val payload = intent.getStringExtra("payload")
            val jsonBody = JSONObject(payload.toString())
            val response = NotificationResponse.fromJsonObject(jsonBody)

            val notifications = getActiveNotifications(arrayOf(response.key))
            if (notifications.isEmpty()) {
                Log.d(LOG_TAG, "Cannot get notification with key $response.key")
                return
            }

            val sbn = notifications[0]

            if (response.actionType == "direct_reply_action" && !response.actionReplyMessage.isNullOrBlank()) {
                directReplyHandler.replyWithMessage(sbn, response.actionTitle, response.actionReplyMessage)

            } else if (response.actionType == "action_button") {
                actionHandler.reply(sbn, response.actionTitle)

            } else {
                throw IllegalArgumentException("Unknown action type: ${response.actionType}")
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error caught: $e")
            e.printStackTrace()
        }
    }
}