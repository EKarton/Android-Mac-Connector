package com.androidmacconnector.androidapp.sms

import android.Manifest
import android.telephony.SmsManager
import android.util.Log
import com.androidmacconnector.androidapp.data.FcmSubscriber
import com.google.firebase.messaging.RemoteMessage

/**
 * A class used to send sms messages
 */
class SmsSenderService {
    fun getRequiredPermissions(): List<String> {
        return arrayListOf(Manifest.permission.SEND_SMS)
    }

    fun sendSmsMessage(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }
}

class SendSmsRequestFcmSubscriber(private val service: SmsSenderService) : FcmSubscriber {
    companion object {
        private const val LOG_TAG = "SendSmsSubscriber"
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

        service.sendSmsMessage(remoteMessage.data["phone_number"]!!, remoteMessage.data["body"]!!)
    }
}