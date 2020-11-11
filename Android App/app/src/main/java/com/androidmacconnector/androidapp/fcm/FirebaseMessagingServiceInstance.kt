package com.androidmacconnector.androidapp.fcm

import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingServiceInstance : FirebaseMessagingService() {
    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    private var subscriptionService: FcmSubscriptionServiceImpl? = null

    override fun onCreate() {
        this.subscriptionService = FcmSubscriptionServiceImpl()
    }

    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "Received new token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        subscriptionService?.onMessageReceived(remoteMessage)
    }


    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}