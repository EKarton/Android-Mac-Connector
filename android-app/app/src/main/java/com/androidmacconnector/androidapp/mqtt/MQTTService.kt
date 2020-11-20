package com.androidmacconnector.androidapp.mqtt

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.ping.PingDeviceReceiver
import com.androidmacconnector.androidapp.sms.messages.ReadSmsMessagesReceiver
import com.androidmacconnector.androidapp.sms.sender.SendSmsReceiver
import com.androidmacconnector.androidapp.sms.threads.ReadSmsThreadsReceiver
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely
import com.google.firebase.auth.FirebaseAuth
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.concurrent.CountDownLatch


class MQTTService: Service() {
    private lateinit var client: MQTTClient

    companion object {
        private const val LOG_TAG = "MqttClientService"
        const val PUBLISH_INTENT_ACTION = "com.androidmacconnector.androidapp.mqtt.intent.action.PUBLISH"
    }

    /**
     * This is called once
     */
    override fun onCreate() {
        Log.d(LOG_TAG, "onCreate()")
        super.onCreate()

        val deviceId = getDeviceIdSafely(this)
        if (deviceId.isNullOrBlank()) {
            Log.d(LOG_TAG, "Cannot find device id")
            return
        }

        val accessToken = getAccessToken()
        if (accessToken.isNullOrBlank()) {
            Log.d(LOG_TAG, "Cannot find access token")
            return
        }

        this.client = MQTTClient(getServerUrl(), deviceId)
        this.client.setUsername(deviceId)
        this.client.setPassword(accessToken)
        this.client.connect()

        this.client.subscribe("$deviceId/${SendSmsReceiver.REQUESTS_TOPIC}", 2) { msg, err ->
            if (msg != null && err == null) {
                sendBroadcastIntent(SendSmsReceiver::class.java, msg)
            }
        }

        this.client.subscribe("$deviceId/${ReadSmsThreadsReceiver.REQUESTS_TOPIC}", 2) { msg, err ->
            if (msg != null && err == null) {
                sendBroadcastIntent(ReadSmsThreadsReceiver::class.java, msg)
            }
        }

        this.client.subscribe("$deviceId/${ReadSmsMessagesReceiver.REQUESTS_TOPIC}", 2) { msg, err ->
            if (msg != null && err == null) {
                sendBroadcastIntent(ReadSmsMessagesReceiver::class.java, msg)
            }
        }

        this.client.subscribe("$deviceId/${PingDeviceReceiver.REQUESTS_TOPIC}", 2) { msg, err ->
            if (msg != null && err == null) {
                sendBroadcastIntent(PingDeviceReceiver::class.java, msg)
            }
        }

        Log.d(LOG_TAG, "onCreate() finished: ${this.client}")
    }

    private fun getServerUrl(): String {
        return Uri.Builder()
            .scheme(getServerProtocol())
            .encodedAuthority(getServerAuthority())
            .build()
            .toString()
    }

    private fun getServerProtocol(): String {
        return this.applicationContext.getString(R.string.mqtt_protocol)
    }

    private fun getServerAuthority(): String {
        return this.applicationContext.getString(R.string.mqtt_authority)
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

    private fun sendBroadcastIntent(cls: Class<*>, message: MqttMessage) {
        val intent = Intent(applicationContext, cls).also { intent ->
            intent.putExtra("payload", String(message.payload))
            intent.putExtra("id", message.id)
            intent.putExtra("is_duplicate", message.isDuplicate)
            intent.putExtra("is_retained", message.isRetained)
            intent.putExtra("qos", message.qos)
        }
        applicationContext.sendBroadcast(intent)
    }

    /**
     * This is called when calling startService()
     * Note: this may get called more than once
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand() with action ${intent?.action} and topic ${intent?.getStringExtra("topic")}")

        if (intent == null) {
            return START_STICKY
        }

        val topic = intent.getStringExtra("topic") ?: return START_STICKY

        if (intent.action == PUBLISH_INTENT_ACTION) {
            intent.getStringExtra("payload")?.let { payload ->
                Log.d(LOG_TAG, "Publishing to topic $topic with payload $payload")
                this.client.publish(topic, payload, 2, true, null)
            }
        }

        return START_STICKY
    }

    /**
     * This is called when the service is destroyed by the OS
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "onDestroy()")

        this.client.disconnect(null)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}