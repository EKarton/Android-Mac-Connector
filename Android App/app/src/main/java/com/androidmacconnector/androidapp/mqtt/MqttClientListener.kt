package com.androidmacconnector.androidapp.mqtt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import com.androidmacconnector.androidapp.sms.sender.SendSmsBroadcastReceiver
import com.androidmacconnector.androidapp.utils.getDeviceId
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttClientListener(private val context: Context) : MqttCallbackExtended {
    companion object {
        private const val LOG_TAG = "MqttClientListener"

        const val SEND_SMS_REQUEST_INTENT = "com.androidmacconnector.androidapp.mqtt.intent.action.SEND_SMS_REQUEST"
        const val SEND_SMS_RESULT_INTENT = "com.androidmacconnector.androidapp.mqtt.intent.action.SEND_SMS_RESULT"
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

        when(topic) {
            "${getDeviceId(context)}/send-sms-request" -> {
                val intent = Intent(context, SendSmsBroadcastReceiver::class.java).also { intent ->
                    intent.putExtra("payload", String(message.payload))
                    intent.putExtra("id", message.id)
                    intent.putExtra("is_duplicate", message.isDuplicate)
                    intent.putExtra("is_retained", message.isRetained)
                    intent.putExtra("qos", message.qos)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}