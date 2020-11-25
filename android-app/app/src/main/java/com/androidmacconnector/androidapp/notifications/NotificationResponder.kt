package com.androidmacconnector.androidapp.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

/**
 * This class is used to respond to notifications,
 * such as liking a post or replying to a text message
 */
class NotificationResponder(private val context: Context) {

    companion object {
        const val LOG_TAG = "NotificationResponder"
    }

    /**
     * Respond to the notification with an action button
     */
    fun reply(sbn: StatusBarNotification, actionTitle: String) {
        // Get the remote inputs (inputs to reply to the notification)
        val wearableExtender = NotificationCompat.WearableExtender(sbn.notification)

        // Find the action with the matching action title
        val action = wearableExtender.actions.find { it.title == actionTitle }
        if (action == null) {
            Log.d(LOG_TAG, "Cannot find action $actionTitle")
            return
        }

        // Create an intent to perform the action
        val localIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Submit the action
        try {
            action.actionIntent.send(this.context, 0, localIntent)

        } catch (e: PendingIntent.CanceledException) {
            Log.e(LOG_TAG, "replyToLastNotification error: " + e.localizedMessage)
        }

        Log.d(LOG_TAG, "Perform notification action!")
    }

    /**
     * Respond to notification with direct reply actions (i.e, reply to notification with text)
     */
    fun replyWithMessage(sbn: StatusBarNotification, actionTitle: String, message: String) {
        // Get the remote inputs (inputs to reply to the notification)
        val wearableExtender = NotificationCompat.WearableExtender(sbn.notification)

        // Find the action with the matching action title
        val action = wearableExtender.actions.find { it.title == actionTitle }
        if (action == null) {
            Log.d(LOG_TAG, "Cannot find action $actionTitle")
            return
        }

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
        try {
            action.actionIntent.send(this.context, 0, localIntent)

        } catch (e: PendingIntent.CanceledException) {
            Log.e(LOG_TAG, "replyToLastNotification error: " + e.localizedMessage)
        }

        Log.d(LOG_TAG, "Sent notification reply!")
    }
}