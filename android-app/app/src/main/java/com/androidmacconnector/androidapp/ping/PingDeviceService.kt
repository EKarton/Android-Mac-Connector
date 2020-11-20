package com.androidmacconnector.androidapp.ping

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.androidmacconnector.androidapp.R

interface PingDeviceService {
    fun dispatchNotification()
}

class PingDeviceServiceImpl(private val context: Context): PingDeviceService {
    companion object {
        const val CHANNEL_ID = "1"
    }

    init {
        setupNotificationChannel()
    }

    /**
     * Sets up the notification channel if needed
     * Note: if the notification channel is already made, it will not do anything
     */
    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = context.getString(R.string.ping_device_channel_name)
            val descriptionText = context.getString(R.string.ping_device_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText

            val notificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    override fun dispatchNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle("Ping device")
            .setContentText("Ping device")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)
    }
}