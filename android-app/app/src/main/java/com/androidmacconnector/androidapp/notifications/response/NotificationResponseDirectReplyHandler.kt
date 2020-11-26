package com.androidmacconnector.androidapp.notifications.response

import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

/**
 * Class used to responds to notification with direct reply actions
 * (i.e, reply to notification with text)
 */
class NotificationResponseDirectReplyHandler(private val context: Context) {
    fun replyWithMessage(sbn: StatusBarNotification, actionTitle: String, message: String) {
        // Get the remote inputs (inputs to reply to the notification)
        val wearableExtender = NotificationCompat.WearableExtender(sbn.notification)

        // Find the action with the matching action title
        val action = wearableExtender.actions.find { it.title == actionTitle }
            ?: throw IllegalArgumentException("Cannot find action $actionTitle")

        // Create an intent to reply to the notification
        val localIntent = Intent()
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val localBundle = sbn.notification.extras
        val remoteInputs = action.remoteInputs

        for (remoteInput in remoteInputs) {
            localBundle.putCharSequence(remoteInput.resultKey, message)
        }

        // Add the bundle and remote inputs to the intent
        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle)

        // Send the reply
        action.actionIntent.send(this.context, 0, localIntent)
    }
}