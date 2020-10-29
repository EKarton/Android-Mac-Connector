package com.androidmacconnector.androidapp.sms.messages

import android.content.Context
import android.util.Log
import com.androidmacconnector.androidapp.fcm.FcmSubscriber
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.google.firebase.messaging.RemoteMessage

class GetSmsMessagesFcmSubscriber(
    private val context: Context,
    private val getSmsMessagesService: GetSmsMessagesService,
    private val publisher: GetSmsMessegesResultsWebPublisher
) : FcmSubscriber {

    companion object {
        private const val LOG_TAG = "GetSmsMessages"
    }

    override fun getMessageAction(): String {
        return "get_sms_messages"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "Message received: ${remoteMessage.data}")

        val deviceId = getDeviceId(context)
        val jobId = remoteMessage.data["uuid"]!!

        try {
            if (remoteMessage.data["thread_id"].isNullOrEmpty()) {
                throw Exception("Missing thread_id in job")
            }

            val threadId = remoteMessage.data["thread_id"]!!

            val messages = getSmsMessagesService.getSmsMessagesFromThread(threadId)
            val result = GetSmsMessagesSuccessfulResult(messages)

            publisher.publishResults(deviceId, jobId, result, object : ResponseHandler() {
                override fun onSuccess() {}
                override fun onError(exception: Exception) {
                    throw exception
                }
            })
        } catch (e: Exception) {
            val result = GetSmsMessagesFailedResult(e.toString())
            publisher.publishResults(deviceId, jobId, result, object : ResponseHandler() {
                override fun onSuccess() {}
                override fun onError(exception: Exception) {
                    throw exception
                }
            })
        }
    }
}