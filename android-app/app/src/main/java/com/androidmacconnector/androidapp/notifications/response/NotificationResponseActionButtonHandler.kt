package com.androidmacconnector.androidapp.notifications.response

import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * This class is used to respond to notifications,
 * such as liking a post
 */
class NotificationResponseActionButtonHandler(private val context: Context) {
    fun reply(sbn: StatusBarNotification, actionTitle: String) {
        // Get the remote inputs (inputs to reply to the notification)
        val wearableExtender = NotificationCompat.WearableExtender(sbn.notification)

        // Find the action with the matching action title
        val action = wearableExtender.actions.find { it.title == actionTitle }
            ?: throw IllegalArgumentException("Cannot find action $actionTitle")

        // Create an intent to perform the action
        val localIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Submit the action
        action.actionIntent.send(this.context, 0, localIntent)
    }
}

