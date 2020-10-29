package com.androidmacconnector.androidapp.fcm

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.androidmacconnector.androidapp.sms.SmsService
import com.androidmacconnector.androidapp.sms.SmsWebService
import com.androidmacconnector.androidapp.sms.*
import com.androidmacconnector.androidapp.sms.sender.OnSendSmsFcmSubscriber
import com.androidmacconnector.androidapp.sms.sender.SendSmsResultsWebPublisher
import com.androidmacconnector.androidapp.sms.sender.SmsSenderService
import com.androidmacconnector.androidapp.sms.threads.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingServiceInstance : FirebaseMessagingService() {
    companion object {
        private const val LOG_TAG = "FcmMessageService"
    }

    private var subscriptionService: FcmSubscriptionServiceImpl? = null
    private var webService: SmsService? = null

    private var smsThreadsWebService: SmsThreadsService? = null
    private var smsThreadsQueryService: SmsThreadsQueryService? = null

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
        checkAndCreateWebService()
        checkAndCreateSmsThreadsService()
        checkAndCreateSmsQueryService()
        checkAndCreateSmsSenderSubscriber()
    }

    private fun checkAndCreateWebService() {
        val createWebService = webService == null &&
                checkPermissions(Manifest.permission.INTERNET)

        if (createWebService) {
            webService = SmsWebService(this)
        }
    }

    private fun checkAndCreateSmsThreadsService() {
        val createSmsThreadsService = (webService != null && smsThreadsQueryService == null) &&
                (checkPermissions(SmsThreadsQueryServiceImpl.getRequiredPermissions()))

        if (createSmsThreadsService) {
            smsThreadsQueryService = SmsThreadsQueryServiceImpl(this.contentResolver)
            smsThreadsWebService = SmsThreadsWebService(this.applicationContext)

            val subscriber = UpdateSmsThreadsFcmSubscriber(
                this.applicationContext,
                smsThreadsQueryService!!,
                smsThreadsWebService!!
            )
            subscriptionService?.addSubscriber(subscriber)
        }
    }

    private fun checkAndCreateSmsQueryService() {
        val createSmsQueryService = (webService != null && smsQueryService == null) &&
                (checkPermissions(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS))

        if (createSmsQueryService) {
            smsQueryService = SmsQueryService(this.contentResolver)
            val subscriber = UpdateSmsForThreadRequestFcmSubscriber(smsQueryService!!, webService!!)
            subscriptionService?.addSubscriber(subscriber)
        }
    }

    private fun checkAndCreateSmsSenderSubscriber() {
        val createSmsSenderService = (webService != null && smsSenderService == null) &&
                checkPermissions(SmsSenderService.getRequiredPermissions())

        if (createSmsSenderService) {
            smsSenderService = SmsSenderService()

            val publishSendSmsResultsService = SendSmsResultsWebPublisher(this.applicationContext)
            val subscriber = OnSendSmsFcmSubscriber(
                this.applicationContext,
                smsSenderService!!,
                publishSendSmsResultsService
            )

            subscriptionService?.addSubscriber(subscriber)
        }
    }


    private fun checkPermissions(permission: List<String>): Boolean {
        return !permission.map {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }.contains(false)
    }

    private fun checkPermissions(vararg permission: String): Boolean {
        return !permission.map {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }.contains(false)
    }
}