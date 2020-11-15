package com.androidmacconnector.androidapp.ping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PingDeviceReceiver : BroadcastReceiver() {

    companion object {
        private const val LOG_TAG = "PingDeviceBR"
    }

    /** This method is called when the BroadcastReceiver is receiving an Intent broadcast. **/
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(LOG_TAG, "Received ping device intent: $intent")
        PingDeviceServiceImpl(context).dispatchNotification()
    }
}