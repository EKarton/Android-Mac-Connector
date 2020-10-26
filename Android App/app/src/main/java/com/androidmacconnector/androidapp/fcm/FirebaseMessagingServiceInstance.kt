package com.androidmacconnector.androidapp.services

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.androidmacconnector.androidapp.sms.SmsService
import com.androidmacconnector.androidapp.sms.SmsWebService
import com.androidmacconnector.androidapp.data.FcmSubscriptionServiceImpl
import com.androidmacconnector.androidapp.sms.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingServiceInstance : FirebaseMessagingService() {
    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    private var subscriptionService: FcmSubscriptionServiceImpl? = null
    private var webService: SmsService? = null
    private var smsQueryService: SmsQueryService? = null
    private var smsSenderService: SmsSenderService? = null

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
        val createWebService = webService == null &&
                checkPermissions(Manifest.permission.INTERNET)

        if (createWebService) {
            webService = SmsWebService(this)
        }

        val createSmsQueryService = (webService != null && smsQueryService == null) &&
                (checkPermissions(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS))

        if (createSmsQueryService) {
            smsQueryService = SmsQueryService(this.contentResolver)

            val subscriber1 = UpdateSmsThreadsRequestFcmSubscriber(smsQueryService!!, webService!!)
            val subscriber2 = UpdateSmsForThreadRequestFcmSubscriber(smsQueryService!!, webService!!)

            subscriptionService?.addSubscriber(subscriber1)
            subscriptionService?.addSubscriber(subscriber2)
        }

        val createSmsSenderService = (webService != null && smsSenderService == null) &&
                checkPermissions(Manifest.permission.SEND_SMS)

        if (createSmsSenderService) {
            smsSenderService = SmsSenderService()

            val subscriber = SendSmsRequestFcmSubscriber(smsSenderService!!, webService!!)

            subscriptionService?.addSubscriber(subscriber)
        }
    }

    private fun checkPermissions(vararg permission: String): Boolean {
        return !permission.map {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }.contains(false)
    }
}