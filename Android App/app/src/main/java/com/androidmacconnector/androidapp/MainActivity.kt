package com.androidmacconnector.androidapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.devices.UpdatePushNotificationTokenHandler
import com.androidmacconnector.androidapp.sms.*
import com.androidmacconnector.androidapp.utils.getDeviceId
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

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

        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)

        val deviceService = DeviceWebService(this)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful || task.result == null) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
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

                    // Upload the fcm token to our server
                    val deviceWebService = DeviceWebService(this)
                    deviceWebService.updatePushNotificationToken(accessToken, deviceId, token, object: UpdatePushNotificationTokenHandler() {
                        override fun onSuccess(response: JSONObject?) {
                            Log.d(TAG, "Successfully updated fcm token")
                        }

                        override fun onError(exception: Exception) {
                            throw exception
                        }
                    })
                }
            }

            // Log and toast
            Log.d(TAG, token)
            textbox.setText(textbox.text.toString() + "\n\n" + token)
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })
    }

    override fun onResume() {
        super.onResume()

        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }
}