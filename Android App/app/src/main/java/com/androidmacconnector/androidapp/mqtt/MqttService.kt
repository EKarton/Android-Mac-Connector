package com.androidmacconnector.androidapp.mqtt

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.google.firebase.auth.FirebaseAuth
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions


class MqttService: Service() {
    private lateinit var client: MqttAsyncClient

    companion object {
        private const val LOG_TAG = "MqttClientService"
        private const val SERVER_URL = "mqtt://192.168.0.102:1883"
    }

    /**
     * This is called once
     */
    override fun onCreate() {
        super.onCreate()
        val message = "MqttService onCreate() method."
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result?.token != null) {
                val accessToken = task.result?.token!!

                Log.d(LOG_TAG, "Access token: $accessToken")

                val clientId = getDeviceId(this)
                this.client = MqttAsyncClient(SERVER_URL, clientId)

                // Connect to the server
                val connectOptions = MqttConnectOptions()
                connectOptions.password = accessToken.toCharArray()
                client.connect(connectOptions, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(LOG_TAG, "Connected successfully")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(LOG_TAG, "Failed to connect")
                    }
                })
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand()")
        val message = "MqttService onStartCommand() method."

        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

        return START_STICKY
    }

    /**
     * This is called when the service is destroyed by the OS
     */
    override fun onDestroy() {
        super.onDestroy()
        this.client.close()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}