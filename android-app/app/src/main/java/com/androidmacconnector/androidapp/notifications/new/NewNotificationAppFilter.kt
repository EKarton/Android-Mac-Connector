package com.androidmacconnector.androidapp.notifications.new

import android.content.Context

class NewNotificationAppFilter(private val context: Context): NewNotificationHandler() {
    override fun onHandleNotification(notification: NewNotification): Boolean {

        // Don't transmit messages from our app
        if (notification.getPackageName() == context.packageName) {
            return false
        }
        return true
    }
}