package com.androidmacconnector.androidapp.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.auth.SignInActivity
import com.androidmacconnector.androidapp.databinding.ActivitySettingsBinding
import com.androidmacconnector.androidapp.devices.RegisterDeviceActivity
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.devices.UpdatedDevice
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.androidmacconnector.androidapp.notifications.AllowNotificationsActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    private lateinit var sessionStore: SessionStoreImpl
    private lateinit var deviceService: DeviceWebServiceImpl
    private lateinit var deviceRegistrationService: DeviceRegistrationService
    private lateinit var binding: ActivitySettingsBinding

    companion object {
        private const val LOG_TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the services
        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        deviceService = DeviceWebServiceImpl(this)
        deviceRegistrationService = DeviceRegistrationService(this, sessionStore, deviceService)

        setupBackButton()
        setupSettingsContent()
        setupButtonListeners()
    }

    override fun onResume() {
        super.onResume()
        setupSettingsContent()
    }

    private fun setupBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupSettingsContent() {
        deviceRegistrationService.getDeviceId { deviceId, err ->
            if (err != null) {
                Log.d(LOG_TAG, "Error getting device id while checking if device is registered or not")
                return@getDeviceId
            }

            binding.isRegistered = !deviceId.isNullOrBlank()
        }

        sessionStore.getUserDetails().let { user ->
            binding.accountName = user?.email
        }

        isDeviceSyncingNotifications { isSyncing ->
            binding.canSyncNotifications = isSyncing
        }
    }

    private fun isDeviceSyncingNotifications(handler: (Boolean) -> Unit) {
        sessionStore.getAuthToken { authToken, err1 ->
            if (err1 != null) {
                Log.d(LOG_TAG, "Error getting auth token: $err1")
                handler(false)
                return@getAuthToken
            }

            deviceRegistrationService.getDeviceId { deviceId, err2 ->
                if (err2 != null || deviceId.isNullOrBlank()) {
                    Log.d(LOG_TAG, "Cannot get device id: $err2")
                    handler(false)
                    return@getDeviceId
                }

                deviceService.getDevice(authToken, deviceId) { device, err3 ->
                    if (err3 != null || device == null) {
                        Log.d(LOG_TAG, "Cannot get device info: $err3")
                        handler(false)
                        return@getDevice
                    }

                    val hasCapability = device.canReceiveNotifications() && device.canRespondToNotifications()
                    val appCanListenToNotifications = canListenToNotifications()

                    handler(hasCapability && appCanListenToNotifications)
                }
            }
        }
    }

    private fun setupButtonListeners() {
        binding.addRemoveDeviceBttn.setOnClickListener { _ ->
            deviceRegistrationService.getDeviceId { deviceId, err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Error getting device id while checking if device is registered or not")
                    return@getDeviceId
                }

                // If it is unregistered, then register the device
                if (deviceId.isNullOrBlank()) {
                    startActivity(Intent(this, RegisterDeviceActivity::class.java))
                    return@getDeviceId
                }

                deviceRegistrationService.unregisterDevice { err2 ->
                    if (err2 != null) {
                        Log.d(LOG_TAG, "Error getting device id while unregistering")
                        return@unregisterDevice
                    }

                    stopService(Intent(this, MQTTService::class.java))

                    binding.isRegistered = false

                    Toast.makeText(this, "Device is unregistered", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.signOutButton.setOnClickListener { _ ->
            sessionStore.signOut()
            stopService(Intent(this, MQTTService::class.java))

            // Go to the sign in page
            startActivity(Intent(this, SignInActivity::class.java))
            finishAffinity()
        }

        binding.setupNotificationSyncButton.setOnClickListener {
            binding.canSyncNotifications?.also { canSync ->
                if (canSync) {
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    startActivityForResult(intent, 1)

                } else {
                    startActivity(Intent(this, AllowNotificationsActivity::class.java))
                }
            }

        }
    }

    /** This is called when the settings activity is returned back to the user */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(LOG_TAG, "Returned from settings activity")

        if (requestCode == 1 && !canListenToNotifications()) {
            removeNotificationCapabilitiesFromCurrentDevice { err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Failed to remove permission to device capabilities: $err")
                }
            }
        }
    }

    private fun canListenToNotifications(): Boolean {
        return Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners") != null &&
                Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners").contains(this.packageName)
    }

    private fun removeNotificationCapabilitiesFromCurrentDevice(handler: (Throwable?) -> Unit) {
        sessionStore.getAuthToken { authToken, err ->
            if (err != null) {
                handler(err)
                return@getAuthToken
            }

            deviceRegistrationService.getDeviceId { deviceId, err2 ->
                if (err2 != null) {
                    handler(err2)
                    return@getDeviceId
                }

                if (deviceId.isNullOrBlank()) {
                    handler(IllegalStateException("Device id is blank"))
                    return@getDeviceId
                }

                deviceService.getDevice(authToken, deviceId) { device, err3 ->
                    if (err3 != null) {
                        handler(err3)
                        return@getDevice
                    }

                    if (device == null) {
                        handler(IllegalStateException("Device is null"))
                        return@getDevice
                    }

                    val updatedCapabilities = device.capabilities.toMutableList()
                    updatedCapabilities.remove("receive_notifications")
                    updatedCapabilities.remove("respond_to_notifications")

                    val updatedProperties = UpdatedDevice(null, null, updatedCapabilities)

                    deviceService.updateDevice(authToken, deviceId, updatedProperties) { err4 ->
                        if (err4 != null) {
                            handler(err4)
                            return@updateDevice
                        }

                        handler(null)
                    }
                }
            }
        }
    }

    /**
     * Is called when the user clicks on the back button
     * It will go back to the previous activity
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}