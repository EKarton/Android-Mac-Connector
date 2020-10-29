package com.androidmacconnector.androidapp.sms.sender

import android.Manifest
import android.telephony.SmsManager

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
