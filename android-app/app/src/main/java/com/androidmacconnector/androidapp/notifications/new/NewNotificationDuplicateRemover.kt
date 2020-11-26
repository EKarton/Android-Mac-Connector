package com.androidmacconnector.androidapp.notifications.new

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Checks if the notification is a duplicate
 * This occurs when a notification is posted twice in the same call
 */
class NewNotificationDuplicateRemover: NewNotificationHandler() {
    private var previousNotification: NewNotification? = null

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun onHandleNotification(notification: NewNotification): Boolean {
        if (notification == previousNotification) {
            return false
        }

        previousNotification = notification
        return true
    }
}