package com.androidmacconnector.androidapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.androidmacconnector.androidapp.auth.SessionServiceImpl
import com.androidmacconnector.androidapp.devices.DeviceListFragment
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.mqtt.MqttService
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = ViewPagerAdapter(this)

        if (getDeviceIdSafely(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        setupNotifications()
        uploadFcmToken()


        Intent(this, MqttService::class.java).also {
            startService(it)
        }
    }

    private fun setupNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun uploadFcmToken() {
        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)

        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful || task.result == null) {
                Log.w(LOG_TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result!!

            // Get the device id
            val deviceId = getDeviceIdSafely(this) ?: return@OnCompleteListener

            val sessionService = SessionServiceImpl()
            sessionService.getAuthToken { authToken, err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Error getting auth token: $err")
                    return@getAuthToken
                }

                if (authToken.isNullOrBlank()) {
                    Log.d(LOG_TAG, "Token is blank!")
                    return@getAuthToken
                }

                val deviceService = DeviceWebService(this)
                deviceService.updatePushNotificationToken2(authToken, deviceId, token) { err2 ->
                    err2?.let {
                        Log.d(LOG_TAG, "Failed to update push notification: $it")
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        // Google play services are required with FCM
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }
}

class ViewPagerAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> DeviceListFragment()
            else -> DeviceListFragment()
        }
    }

    override fun getItemCount(): Int {
        return 1
    }
}