package com.androidmacconnector.androidapp.mqtt

import android.content.Context
import android.content.Intent
import android.util.Log
import com.androidmacconnector.androidapp.sms.messages.GetSmsMessagesBroadcastReceiver
import com.androidmacconnector.androidapp.sms.sender.SendSmsBroadcastReceiver
import com.androidmacconnector.androidapp.sms.threads.GetSmsThreadsBroadcastReceiver
import com.androidmacconnector.androidapp.utils.getDeviceId
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttClientListener(private val context: Context) : MqttCallbackExtended {
    companion object {
        private const val LOG_TAG = "MqttClientListener"
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
     * Called when our client successfully published a message
     */
    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d(LOG_TAG, "Delivery complete $token")
    }

    /**
     * Called when our client receives a message from a subscription
     */
    override fun messageArrived(topic: String?, message: MqttMessage?) {
        Log.d(LOG_TAG, "Message arrived from $topic with message $message")
        if (topic == null || message == null) {
            return
        }

        val deviceId = getDeviceId(context)
        val intent: Intent = when(topic) {
            "$deviceId/send-sms-request" -> {
                createBroadcastIntent(SendSmsBroadcastReceiver::class.java, message)
            }
            "${deviceId}/sms/threads/query-requests" -> {
                createBroadcastIntent(GetSmsThreadsBroadcastReceiver::class.java, message)
            }
            "${deviceId}/sms/messages/query-requests" -> {
                createBroadcastIntent(GetSmsMessagesBroadcastReceiver::class.java, message)
            }
            else -> {
                throw IllegalArgumentException("Did not handle topic $topic")
            }
        }
        context.sendBroadcast(intent)
    }

    private fun createBroadcastIntent(cls: Class<*>, message: MqttMessage): Intent {
        return Intent(context, cls).also { intent ->
            intent.putExtra("payload", String(message.payload))
            intent.putExtra("id", message.id)
            intent.putExtra("is_duplicate", message.isDuplicate)
            intent.putExtra("is_retained", message.isRetained)
            intent.putExtra("qos", message.qos)
        }
    }
}