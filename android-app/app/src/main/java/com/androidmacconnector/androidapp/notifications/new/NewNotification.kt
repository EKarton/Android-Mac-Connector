package com.androidmacconnector.androidapp.notifications.new

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class NewNotification(private val sbn: StatusBarNotification) {

    fun getStatusBarNotification(): StatusBarNotification {
        return sbn
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun getKey(): String {
        return sbn.key
    }

    fun getPackageName(): String {
        return sbn.packageName
    }

    fun getAppName(context: Context): String? {
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, PackageManager.GET_META_DATA)
            return packageManager.getApplicationLabel(appInfo).toString()

        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }

    fun getTimePosted(): Long {
        return sbn.notification.`when`
    }

    fun getTitle(): String? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras

        // The content title is from the bundle too
        // https://developer.android.com/reference/android/app/Notification#EXTRA_TITLE
        return extras.getString(NotificationCompat.EXTRA_TITLE)
    }

    fun getContentText(): String? {
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

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun getActions(): List<NewNotificationActions> {
        if (sbn.notification.actions == null) {
            return emptyList()
        }

        val actions = mutableListOf<NewNotificationActions>()
        for (action in sbn.notification.actions) {
            if (action.title == null) {
                continue
            }

            if (action.remoteInputs == null || action.remoteInputs.isEmpty()) {
                actions.add(NewNotificationActionButton(action.title.toString()))
            } else {
                actions.add(NewNotificationDirectReplyAction(action.title.toString()))
            }
        }
        return actions
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun equals(other: Any?): Boolean {
        if (other is NewNotification) {
            return this.getKey() == other.getKey() &&
                    this.getPackageName() == other.getPackageName() &&
                    this.getTimePosted() == other.getTimePosted()
        }

        return false
    }
}

open class NewNotificationActions(
    val type: String,
    val text: String
)
class NewNotificationActionButton(text: String): NewNotificationActions("action_button", text)
class NewNotificationDirectReplyAction(text: String): NewNotificationActions("direct_reply_action", text)
