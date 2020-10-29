package com.androidmacconnector.androidapp.sms.sender

import android.content.Context
import android.util.Log
import com.androidmacconnector.androidapp.fcm.FcmSubscriber
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.google.firebase.messaging.RemoteMessage

class SendSmsFcmSubscriber(
    private val context: Context,
    private val sendSmsService: SendSmsServiceImpl,
    private val resultPublisher: SendSmsResultsPublisher
) : FcmSubscriber {

    companion object {
        private const val LOG_TAG = "SendSmsSub"
    }

    override fun getMessageAction(): String {
        return "send_sms"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "${remoteMessage.data}")

        if (remoteMessage.data["phone_number"].isNullOrBlank()) {
            Log.e(LOG_TAG, "Phone number is empty: ${remoteMessage.data["phone_number"]}")
            return
        }

        if (remoteMessage.data["body"].isNullOrBlank()) {
            Log.e(LOG_TAG, "Body is empty: ${remoteMessage.data["body"]}")
            return
        }

        val deviceId = getDeviceId(context)
        val jobId = remoteMessage.data["uuid"]!!
        val phoneNumber = remoteMessage.data["phone_number"]!!
        val message = remoteMessage.data["body"]!!

        try {
            sendSmsService.sendSmsMessage(phoneNumber, message)
            resultPublisher.publishResults(deviceId, jobId, SendSmsSuccessfulResults(), object: PublishResultsHandler(){
                override fun onSuccess() {}
                override fun onError(exception: Exception) {
                    throw exception
                }
            })
        } catch (e: Exception) {
            val result = SendSmsFailedResults(e.toString())
            resultPublisher.publishResults(deviceId, jobId, result, object: PublishResultsHandler(){
                override fun onSuccess() {}
                override fun onError(exception: Exception) {
                    throw exception
                }
            })
        }
    }
}