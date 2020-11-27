package com.androidmacconnector.androidapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.auth.SignInActivity
import com.androidmacconnector.androidapp.databinding.ActivityMainBinding
import com.androidmacconnector.androidapp.devices.DeviceActionsFragment
import com.androidmacconnector.androidapp.devices.DeviceListFragment
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.androidmacconnector.androidapp.settings.SettingsActivity
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionStore: SessionStoreImpl
    private lateinit var deviceService: DeviceWebServiceImpl
    private lateinit var deviceRegistrationService: DeviceRegistrationService

    companion object {
        private const val LOG_TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the binding and the view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        deviceService = DeviceWebServiceImpl(this)
        deviceRegistrationService = DeviceRegistrationService(this, sessionStore, deviceService)

        if (!sessionStore.isSignedIn()) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setSupportActionBar(binding.mainToolbar)

        setupTabs()
        setupNavMenu()
        setupNotifications()
        uploadFcmToken()

        Intent(this, MQTTService::class.java).also {
            startService(it)
        }

//        if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners") != null) {
//            if (Settings.Secure.getString(this.contentResolver,"enabled_notification_listeners").contains(applicationContext.packageName)) {
//                // service is enabled do nothing
//            } else {
//                // service is not enabled try to enabled
//                applicationContext.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
//            }
//        } else {
//            Log.d(LOG_TAG, "onResume no Google Play Services");
//        }
    }

    private fun setupTabs() {
        // Bind the view pager to the tabs so that when the user clicks on the tab, it changes the view pager
        // Refer to https://developer.android.com/guide/navigation/navigation-swipe-view-2
        binding.viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Actions"
                1 -> "Devices"
                else -> throw IllegalStateException("There is a max. 2 tabs")
            }
        }.attach()

        // Bind the action bar's text to the tab's text
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                supportActionBar?.title = tab?.text
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupNavMenu() {
        // Make the drawer open and close from the nav menu icon in the action bar
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawer,
            binding.mainToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawer.addDrawerListener(toggle)

        // Add a listener to when the user selects an item from the nav drawer
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.actionsMenuItem -> binding.viewPager.setCurrentItem(0, true)
                R.id.devicesMenuItem -> binding.viewPager.setCurrentItem(1, true)
                R.id.settingsMenuItem -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            binding.drawer.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
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
            deviceRegistrationService.getDeviceId { deviceId, err ->
                if (err != null || deviceId.isNullOrBlank()) {
                    Log.d(LOG_TAG, "Error getting device id: $err")
                    return@getDeviceId
                }

                sessionStore.getAuthToken { authToken, err2 ->
                    if (err2 != null || authToken.isBlank()) {
                        Log.d(LOG_TAG, "Error getting auth token: $err")
                        return@getAuthToken
                    }

                    deviceService.updatePushNotificationToken(authToken, deviceId, token) { err3 ->
                        if (err3 != null) {
                            Log.d(LOG_TAG, "Failed to update push notification: $err3")
                        }
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