package com.androidmacconnector.androidapp.fcm

import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.androidmacconnector.androidapp.sms.messages.*
import com.androidmacconnector.androidapp.sms.threads.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.Manifest

class FirebaseMessagingServiceInstance : FirebaseMessagingService() {
    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    private var subscriptionService: FcmSubscriptionServiceImpl? = null

    private var getGetSmsThreadsService: GetSmsThreadsService? = null
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
            checkAndCreateSmsThreadsService()
            checkAndCreateSmsQueryService()
        }
    }

    private fun checkAndCreateSmsThreadsService() {
        if (getGetSmsThreadsService == null && checkPermissions(GetSmsThreadsServiceImpl.getRequiredPermissions())) {
            getGetSmsThreadsService = GetSmsThreadsServiceImpl(this.contentResolver)

            val publisher = GetSmsThreadsResultWebPublisher(this.applicationContext)
            val subscriber = GetSmsThreadsFcmSubscriber(
                this.applicationContext,
                getGetSmsThreadsService!!,
                publisher
            )

            subscriptionService?.addSubscriber(subscriber)
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