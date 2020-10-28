package com.androidmacconnector.androidapp.sms

import android.Manifest
import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.androidmacconnector.androidapp.fcm.FcmSubscriber
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

/**
 * A class used to send sms messages
 */
class SmsSenderService {
    companion object {
        fun getRequiredPermissions(): List<String> {
            return arrayListOf(Manifest.permission.SEND_SMS)
        }
    }

    fun sendSmsMessage(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }
}

class SendSmsRequestFcmSubscriber(private val context: Context, private val service: SmsSenderService, private val webService: SmsService) : FcmSubscriber {
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

        if (remoteMessage.data["uuid"].isNullOrBlank()) {
            Log.e(LOG_TAG, "Uuid is empty: ${remoteMessage.data["uuid"]}")
            return
        }

        val deviceId = getDeviceId(context)

        try {
            service.sendSmsMessage(remoteMessage.data["phone_number"]!!, remoteMessage.data["body"]!!)

            webService.updateSmsMessageSentStatus(deviceId, remoteMessage.data["uuid"]!!, "sent", object : UpdateSmsSentStatusHandler() {
                override fun onSuccess() {}
                override fun onError(exception: Exception) {
                    throw exception
                }
            })
        } catch (e: Exception) {
            webService.updateSmsMessageSentStatus(deviceId, remoteMessage.data["uuid"]!!, "failed", object : UpdateSmsSentStatusHandler() {
                override fun onSuccess() {}
                override fun onError(exception: Exception) {
                    throw exception
                }
            })
        }
    }
}