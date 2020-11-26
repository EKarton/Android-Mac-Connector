package com.androidmacconnector.androidapp.notifications.new

import android.app.Notification
import android.util.Log

/**
 * Checks if the notification is a notification that we want to publish
 * There are notifications that we don't want to publish, such as notification histories,
 * ongoing events (like downloads), notifications from foreground services, etc.
 */
class NewNotificationInvalidFlagsRemover: NewNotificationHandler() {

    companion object {
        private const val LOG_TAG = "NewNotFlagsHandler"
    }

    override fun onHandleNotification(notification: NewNotification): Boolean {
        val originalNot = notification.getStatusBarNotification().notification

        // Ignore events that represent foreground services
        if ((originalNot.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) {
            Log.d(LOG_TAG, "Notification is foreground services")
            return false
        }

        // Ignore ongoing events like downloads
        if ((originalNot.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            Log.d(LOG_TAG, "Notification is ongoing event")
            return false
        }

        if ((originalNot.flags and Notification.FLAG_LOCAL_ONLY) != 0) {
            Log.d(LOG_TAG, "Notification is local only")
            return false
        }

        // There is a case where gmail apps send duplicate notifications
        // link: https://stackoverflow.com/questions/45890487/android-onnotificationposted-is-called-twice-for-gmail-and-whatsapp
        if ((originalNot.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            Log.d(LOG_TAG, "Notification is group summary")
            return false
        }

        // There are cases where new notifications are made on top of one another
        // but Android sends this twice. We ignore this
        if ((originalNot.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0) {
            Log.d(LOG_TAG, "Notification is already alerted once")
            return false
        }

        return true
    }
}