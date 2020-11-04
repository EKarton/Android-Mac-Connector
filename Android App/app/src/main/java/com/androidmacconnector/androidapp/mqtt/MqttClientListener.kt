package com.androidmacconnector.androidapp.mqtt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import com.androidmacconnector.androidapp.utils.getDeviceId
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttClientListener(private val context: Context) : MqttCallbackExtended {
    companion object {
        private const val LOG_TAG = "MqttClientListener"

        const val SEND_SMS_INTENT = "com.androidmacconnector.androidapp.mqtt.intent.action.SEND_SMS"
    }

    /**
     * Called when our client loses connection to the server
     */
    override fun connectionLost(cause: Throwable?) {
        Log.d(LOG_TAG, "Connection lost")
    }

    /**
     * Called when our client connects to the server successfully
     */
    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        Log.d(LOG_TAG, "Connection complete (reconnect? $reconnect)")
    }

    /**
     * Called when our app / service successfully published a message
     */
    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d(LOG_TAG, "Delivery complete $token")
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        Log.d(LOG_TAG, "Message arrived from $topic with message $message")
        if (topic == null || message == null) {
            return
        }

        Intent().also { intent ->
            intent.action = this.getIntentAction(topic)
            intent.putExtra("message", message.payload)
            context.sendBroadcast(intent, this.getIntentPermission(topic))
        }
    }

    private fun getIntentAction(topic: String): String? {
        when(topic) {
            "${getDeviceId(context)}/send-sms-request" -> return SEND_SMS_INTENT
            else -> {
                return null
            }
        }
    }

    private fun getIntentPermission(topic: String): String? {
        when(topic) {
            "${getDeviceId(context)}/send-sms-request" -> return Manifest.permission.SEND_SMS
            else -> {
                return null
            }
        }
    }
}