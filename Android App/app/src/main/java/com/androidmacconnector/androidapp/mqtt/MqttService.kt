package com.androidmacconnector.androidapp.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely
import com.google.firebase.auth.FirebaseAuth
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.CountDownLatch


class MqttService: Service() {
    private lateinit var client: MqttAsyncClient

    companion object {
        private const val LOG_TAG = "MqttClientService"
        private const val SERVER_URL = "ws://192.168.0.102:8888"
        const val PUBLISH_INTENT_ACTION = "com.androidmacconnector.androidapp.mqtt.intent.action.PUBLISH"
        const val SUBSCRIBE_INTENT_ACTION = "com.androidmacconnector.androidapp.mqtt.intent.action.SUBSCRIBE"
    }

    /**
     * This is called once
     */
    override fun onCreate() {
        super.onCreate()

        this.setupClient()
        this.setupSubscriptions()
    }

    private fun setupClient() {
        Log.d(LOG_TAG, "Setting up client")
        val deviceId = getDeviceIdSafely(this) ?: return
        this.client = MqttAsyncClient(SERVER_URL, deviceId, MemoryPersistence())
        this.client.setCallback(MqttClientListener(this))

        // Set up the disconnected buffer
        val disconnectedBufferOptions = DisconnectedBufferOptions()
        disconnectedBufferOptions.isBufferEnabled = true
        disconnectedBufferOptions.isDeleteOldestMessages = true
        disconnectedBufferOptions.isPersistBuffer = true
        this.client.setBufferOpts(disconnectedBufferOptions)

        // Set up connection to the server
        val connectOptions = MqttConnectOptions()
        connectOptions.userName = deviceId
        connectOptions.password = (this.getAccessToken() ?: "").toCharArray()
        connectOptions.isCleanSession = false
        connectOptions.isAutomaticReconnect = true

        // Connect to the server
        val latch = CountDownLatch(1)
        this.client.connect(connectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(LOG_TAG, "Connected successfully")
                latch.countDown()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                throw exception!!
            }
        })

        latch.await()
    }

    private fun setupSubscriptions() {
        Log.d(LOG_TAG, "Setting up subscriptions")
        this.client.subscribe("${getDeviceId(this)}/sms/send-message-requests", 2, )
        this.client.subscribe("${getDeviceId(this)}/sms/threads/query-requests", 2)
        this.client.subscribe("${getDeviceId(this)}/sms/messages/query-requests", 2)
    }

    private fun getAccessToken(): String? {
        val latch = CountDownLatch(1)
        val tokenResult = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnCompleteListener { _ ->
            latch.countDown()
        }
        if (tokenResult != null && tokenResult.isSuccessful && tokenResult.result?.token != null) {
            return tokenResult.result?.token!!
        }
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand() with action ${intent?.action} and topic ${intent?.getStringExtra("topic")}")

        if (intent == null) {
            return START_STICKY
        }

        val topic = intent.getStringExtra("topic") ?: return START_STICKY

        if (intent.action == PUBLISH_INTENT_ACTION) {
            val payload = intent.getByteArrayExtra("payload")

            Log.d(LOG_TAG, "Publishing to topic $topic with payload $payload")
            this.client.publish(topic, payload, 2, true)

        } else if (intent.action == SUBSCRIBE_INTENT_ACTION) {
            Log.d(LOG_TAG, "Subscribing topic $topic")
            this.client.subscribe(topic, 2)
        }

        return START_STICKY
    }

    /**
     * This is called when the service is destroyed by the OS
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "onDestroy()")

        this.client.disconnect()
        this.client.close()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}