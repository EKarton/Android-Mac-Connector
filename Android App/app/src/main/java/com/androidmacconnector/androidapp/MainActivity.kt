package com.androidmacconnector.androidapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.devices.UpdatePushNotificationTokenHandler
import com.androidmacconnector.androidapp.mqtt.MqttService
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textbox = findViewById<EditText>(R.id.myTextBox)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW)
            )
        }

        if (getDeviceIdSafely(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful || task.result == null) {
                Log.w(LOG_TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result!!

            // Get the device id
            val deviceId = getDeviceId(this)

            // Get the access token
            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(false)?.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result?.token != null) {
                    val accessToken = task.result?.token!!

                    Log.d(LOG_TAG, "Access token: $accessToken")

                    // Upload the fcm token to our server
                    val deviceWebService = DeviceWebService(this)
                    deviceWebService.updatePushNotificationToken(accessToken, deviceId, token, object: UpdatePushNotificationTokenHandler() {
                        override fun onSuccess() {
                            Log.d(LOG_TAG, "Successfully updated fcm token")
                        }

                        override fun onError(exception: Exception) {
                            throw exception
                        }
                    })
                }
            }

            // Log and toast
            Log.d(LOG_TAG, token)
            textbox.setText(textbox.text.toString() + "\n\n" + token)
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })

        Intent(this, MqttService::class.java).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(LOG_TAG, "Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            Log.d(LOG_TAG, "Starting the service in < 26 Mode")
            startService(it)
        }
    }

    override fun onResume() {
        super.onResume()

        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }
}