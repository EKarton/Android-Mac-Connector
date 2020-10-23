package com.androidmacconnector.androidapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.data.*
import com.androidmacconnector.androidapp.services.SmsBroadcastReceiver
import com.androidmacconnector.androidapp.sms.*
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textbox = findViewById<EditText>(R.id.myTextBox)

        val smsQueryService = SmsQueryService(this.contentResolver)
        val smsSenderService = SmsSenderService()

        val requiredPermissions = smsQueryService.getRequiredPermissions() +
                smsSenderService.getRequiredPermissions() +
                arrayListOf(Manifest.permission.RECEIVE_SMS)

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

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            Log.d(TAG, token!!)
            textbox.setText(textbox.text.toString() + "\n\n" + token)
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })

        Dexter.withContext(this)
            .withPermissions(requiredPermissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    Log.d(TAG, "Permissions granted")
                }

                override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest?>?, token: PermissionToken?) {
                    Log.d(TAG, "Permissions not granted")
                    token?.continuePermissionRequest();
                }
            })
            .onSameThread()
            .check()
    }

    override fun onResume() {
        super.onResume()

        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }
}