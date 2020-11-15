package com.androidmacconnector.androidapp.ping

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.ping.PingDeviceService.Companion.CHANNEL_ID


interface PingDeviceService {
    companion object {
        const val CHANNEL_ID = "1"
        const val PERMISSION = "com.androidmacconnector.androidapp.permissions.ping_device"
    }

    fun setupNotificationChannel()
    fun dispatchNotification()
}

class PingDeviceServiceImpl(private val context: Context): PingDeviceService {
    override fun setupNotificationChannel() {
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