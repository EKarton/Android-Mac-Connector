package com.androidmacconnector.androidapp.sms.threads

import android.content.Context
import android.util.Log
import com.androidmacconnector.androidapp.fcm.FcmSubscriber
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.google.firebase.messaging.RemoteMessage

class UpdateSmsThreadsFcmSubscriber(private val context: Context,
                                    private val smsQueryService: SmsThreadsQueryService,
                                    private val webService: SmsThreadsService
) : FcmSubscriber {
    companion object {
        private const val LOG_TAG = "UpdateSmsThreads"
    }

    override fun getMessageAction(): String {
        return "update_sms_threads"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "Message received: ${remoteMessage.data}")

        val deviceId = getDeviceId(context)
        val jobId = remoteMessage.data["uuid"]!!

        if (remoteMessage.data["limit"].isNullOrBlank()) {
            throw Exception("It should have a limit value")
        }

        if (remoteMessage.data["start"].isNullOrBlank()) {
            throw Exception("It should have a start value")
        }

        val limit = remoteMessage.data["limit"]!!.toInt()
        val start = remoteMessage.data["start"]!!.toInt()

        try {
            val threads = smsQueryService.getSmsThreadsSummary(limit, start)
            val results = SmsThreadsQuerySuccessfulResults(threads)
            webService.publishResults(deviceId, jobId, results, object : PublishResultsHandler() {
                override fun onSuccess() {}
                override fun onError(exception: Exception) {
                    throw exception
                }
            })
        } catch (e: Exception) {
            val results = SmsThreadsQueryFailedResults(e.toString())
            webService.publishResults(deviceId, jobId, results, object : PublishResultsHandler() {
                override fun onSuccess() {}
                override fun onError(exception: Exception) {
                    throw exception
                }
            })
        }
    }
}