package com.androidmacconnector.androidapp.sms.sender

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.androidmacconnector.androidapp.mqtt.MqttClientListener.Companion.SEND_SMS_INTENT


class SendSmsBroadcastReceiver: BroadcastReceiver() {
    companion object {
        private const val LOG_TAG = "SendSmsBcReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action == SEND_SMS_INTENT) {
            Log.d(LOG_TAG, "Received send sms intent")
        }
    }
}