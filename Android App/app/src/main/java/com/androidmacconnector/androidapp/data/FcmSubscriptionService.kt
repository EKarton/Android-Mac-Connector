package com.androidmacconnector.androidapp.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.androidmacconnector.androidapp.MainActivity
import com.androidmacconnector.androidapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * This class represents a Firebase Cloud Messaging (Fcm) subscriber,
 * where FCM messages get allocated to their appropriate subscribers
 */
interface FcmSubscriptionService {
    fun addSubscriber(subscriber: FcmSubscriber)
}

class FcmSubscriptionServiceImpl : FirebaseMessagingService(), FcmSubscriptionService {

    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    private val actionToSubscribers = HashMap<String, HashSet<FcmSubscriber>>()

    override fun addSubscriber(subscriber: FcmSubscriber) {
        val action = subscriber.getMessageAction()

        if (!actionToSubscribers.containsKey(action)) {
            actionToSubscribers[action] = HashSet()
        }

        actionToSubscribers[action]?.add(subscriber)
    }

    /**
     * Is called when a new token has been received for FCM
     */
    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "Received new token: $token")
    }

    /**
     * Is called when a new message is received to this device
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "Received new message: ${remoteMessage.data}")

        // Send a notification if the message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(LOG_TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.body!!)
        }

        // Check if it has a data entry, and if so, call the appropriate subscribers
        if (remoteMessage.data.isNotEmpty()) {
            val action = remoteMessage.data["action"]

            actionToSubscribers[action]?.forEach {
                it.getHandler()(remoteMessage)
            }
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     */
    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle("Hello there!")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}

abstract class FcmSubscriber(private val handler: (RemoteMessage) -> Unit) {
    internal fun getHandler(): (RemoteMessage) -> Unit {
        return handler
    }

    abstract fun getMessageAction(): String
}

class SendSmsRequestFcmSubscriber(handler: (RemoteMessage) -> Unit) : FcmSubscriber(handler) {
    override fun getMessageAction(): String {
        return "send_sms"
    }
}

class UpdateSmsThreadsRequestFcmSubscriber(handler: (RemoteMessage) -> Unit) :
    FcmSubscriber(handler) {
    override fun getMessageAction(): String {
        return "update_sms_threads"
    }
}

class UpdateSmsForThreadRequestFcmSubscriber(handler: (RemoteMessage) -> Unit) :
    FcmSubscriber(handler) {
    override fun getMessageAction(): String {
        return "update_sms_thread_messages"
    }
}