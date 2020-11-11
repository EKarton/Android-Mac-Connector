package com.androidmacconnector.androidapp.fcm

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.androidmacconnector.androidapp.sms.messages.GetSmsMessagesFcmSubscriber
import com.androidmacconnector.androidapp.sms.messages.GetSmsMessagesService
import com.androidmacconnector.androidapp.sms.messages.GetSmsMessagesServiceImpl
import com.androidmacconnector.androidapp.sms.messages.GetSmsMessegesResultsWebPublisher
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingServiceInstance : FirebaseMessagingService() {
    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    private var subscriptionService: FcmSubscriptionServiceImpl? = null
    private var getSmsMessagesService: GetSmsMessagesService? = null

    override fun onCreate() {
        this.subscriptionService = FcmSubscriptionServiceImpl()
    }

    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "Received new token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        checkAndCreateSubscribers()
        subscriptionService?.onMessageReceived(remoteMessage)
    }

    private fun checkAndCreateSubscribers() {
        if (checkPermission(Manifest.permission.INTERNET)) {
            checkAndCreateSmsQueryService()
        }
    }

    private fun checkAndCreateSmsQueryService() {
        if (getSmsMessagesService == null && checkPermissions(GetSmsMessagesServiceImpl.getRequiredPermissions())) {
            getSmsMessagesService = GetSmsMessagesServiceImpl(this.contentResolver)

            val publisher = GetSmsMessegesResultsWebPublisher(this.applicationContext)
            val subscriber = GetSmsMessagesFcmSubscriber(
                this.applicationContext,
                getSmsMessagesService!!,
                publisher
            )

            subscriptionService?.addSubscriber(subscriber)
        }
    }


    private fun checkPermissions(permission: List<String>): Boolean {
        return !permission.map { checkPermission(it) }.contains(false)
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}