package com.androidmacconnector.androidapp.data

import android.util.Log
import com.google.firebase.messaging.RemoteMessage

interface SubscriptionService {
    fun addSubscriber(subscriber: FcmSubscriber)
}

/**
 * This class represents a Firebase Cloud Messaging (Fcm) subscriber,
 * where FCM messages get allocated to their appropriate subscribers
 */
class FcmSubscriptionServiceImpl : SubscriptionService {

    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    private val actionToSubscribers = HashMap<String, HashSet<FcmSubscriber>>()

    override fun addSubscriber(subscriber: FcmSubscriber) {
        val action = subscriber.getMessageAction()

        if (actionToSubscribers[action] == null) {
            actionToSubscribers[action] = HashSet()
        }

        actionToSubscribers[action]?.add(subscriber)

        Log.d(LOG_TAG, actionToSubscribers.keys.toString())
    }

    /**
     * Is called when a new message is received to this device
     */
    internal fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "Received new message: ${remoteMessage.data}")

        // Send a notification if the message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(LOG_TAG, "Message Notification Body: ${it.body}")
        }

        // Check if it has a data entry, and if so, call the appropriate subscribers
        if (remoteMessage.data.isNotEmpty()) {
            val action = remoteMessage.data["action"]

            actionToSubscribers[action]?.forEach {
                it.onMessageReceived(remoteMessage)
            }
        }
    }
}

interface FcmSubscriber {

    /**
     * Returns which action this subscriber is responsible for
     */
    fun getMessageAction(): String

    /**
     * Called when it receives a message with an action that matches this.getMessageAction()
     */
    fun onMessageReceived(remoteMessage: RemoteMessage)
}