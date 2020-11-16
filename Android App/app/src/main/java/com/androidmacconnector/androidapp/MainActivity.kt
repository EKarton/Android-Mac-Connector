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
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.auth.SignInActivity
import com.androidmacconnector.androidapp.devices.DeviceActionsFragment
import com.androidmacconnector.androidapp.devices.DeviceListFragment
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import java.lang.IllegalStateException


class MainActivity : AppCompatActivity() {
    private lateinit var sessionStore: SessionStoreImpl

    companion object {
        private const val LOG_TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())

        if (!sessionStore.isSignedIn()) {
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
            return
        }

        setupTabs()
        setupNotifications()
        uploadFcmToken()

        Intent(this, MQTTService::class.java).also {
            startService(it)
        }
    }

    private fun setupTabs() {
        // Bind the view pager to the tabs so that when the user clicks on the tab, it changes the view pager
        // Refer to https://developer.android.com/guide/navigation/navigation-swipe-view-2
        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = ViewPagerAdapter(this)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Actions"
                1 -> "Devices"
                else -> throw IllegalStateException("There is a max. 2 tabs")
            }
        }.attach()
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
            sessionStore.getAuthToken { authToken, err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Error getting auth token: $err")
                    return@getAuthToken
                }

                val deviceService = DeviceWebService(this)
                deviceService.updatePushNotificationToken(authToken, deviceId, token) { err2 ->
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
    companion object {
        private const val LOG_TAG = "ViewPagerAdapter"
    }

    override fun createFragment(position: Int): Fragment {
        Log.d(LOG_TAG, position.toString())

        return when(position){
            0 -> DeviceActionsFragment()
            1 -> DeviceListFragment()
            else -> throw IllegalStateException("There is a max. 2 tabs")
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}