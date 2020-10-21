package com.androidmaccontinuity.androidapp.sms

import android.Manifest
import android.telephony.SmsManager

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