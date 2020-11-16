package com.androidmacconnector.androidapp.ping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PingDeviceReceiver : BroadcastReceiver() {

    companion object {
        const val LOG_TAG = "PingDeviceReceiver"
        const val REQUESTS_TOPIC = "ping/requests"
    }

    /** This method is called when the BroadcastReceiver is receiving an Intent broadcast. **/
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(LOG_TAG, "Received ping device intent: $intent")
        PingDeviceServiceImpl(context).dispatchNotification()
    }
}