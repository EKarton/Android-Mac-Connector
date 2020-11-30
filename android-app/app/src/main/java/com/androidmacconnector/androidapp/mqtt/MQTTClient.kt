package com.androidmacconnector.androidapp.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * This class is an abstraction of the MqttAsyncClient library
 */
class MQTTClient(url: String, clientId: String): MqttCallbackExtended {

    companion object {
        private const val LOG_TAG = "MQTTClient"
    }

    private var client: MqttAsyncClient
    private var connectOptions: MqttConnectOptions

    init {
        // Set up the client
        this.client = MqttAsyncClient(url, clientId, MemoryPersistence())
        this.client.setCallback(this)

        // Set up the disconnected buffer
        val disconnectedBufferOptions = DisconnectedBufferOptions()
        disconnectedBufferOptions.isBufferEnabled = true
        disconnectedBufferOptions.isDeleteOldestMessages = true
        disconnectedBufferOptions.isPersistBuffer = true
        this.client.setBufferOpts(disconnectedBufferOptions)

        // Set up the connection options
        this.connectOptions = MqttConnectOptions()
        connectOptions.isCleanSession = false
        connectOptions.maxInflight = 100
        connectOptions.isAutomaticReconnect = true
    }

    fun setUsername(username: String) {
        this.connectOptions.userName = username
    }

    fun setPassword(password: String) {
        this.connectOptions.password = password.toCharArray()
    }

    fun connect() {
        this.client.connect(connectOptions).waitForCompletion()
    }

    fun isConnected(): Boolean {
        return this.client.isConnected
    }

    fun subscribe(topic: String, qos: Int, callback: ((MqttMessage?, Throwable?) -> Unit)?) {
        val actionListener = object: IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(LOG_TAG, "Successfully subscribed to $topic")
                callback?.invoke(null, null)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(LOG_TAG, "Failed to subscribe to $topic")
                callback?.invoke(null, exception)
            }
        }

        val messageListener = IMqttMessageListener { incomingTopic, message ->
            if (incomingTopic == topic) {
                callback?.invoke(message, null)
            }
        }

        this.client.subscribe(topic, qos, null, actionListener, messageListener)
    }

    fun unsubscribe(topic: String, callback: ((Throwable?) -> Unit)?) {
        val listener = object: IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                callback?.invoke(null)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                callback?.invoke(exception)
            }

        }
        this.client.unsubscribe(topic, null, listener)
    }

    fun publish(topic: String, payload: String, qos: Int, retained: Boolean, callback: ((Throwable?) -> Unit)?) {
        val listener = object: IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                callback?.invoke(null)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                callback?.invoke(exception)
            }

        }
        this.client.publish(topic, payload.toByteArray(), qos, retained, null, listener)
    }

    fun disconnect() {
        this.client.disconnect(null, null).waitForCompletion()
    }

    override fun connectionLost(cause: Throwable?) {
        Log.d(LOG_TAG, "Connection lost from $cause")
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        if (topic == null || message == null) {
            return
        }

        Log.d(LOG_TAG, "Message arrived from $topic " +
                "\n payload:$message " +
                "\n qos: ${message.qos} " +
                "\n isRetained: ${message.isRetained} " +
                "\n isDup: ${message.isDuplicate}"
        )
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d(LOG_TAG, "Delivery complete $token")
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        Log.d(LOG_TAG, "Connection complete (reconnect? $reconnect)")
    }
}