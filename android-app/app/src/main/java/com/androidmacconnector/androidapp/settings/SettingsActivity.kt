package com.androidmacconnector.androidapp.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.auth.SignInActivity
import com.androidmacconnector.androidapp.databinding.ActivitySettingsBinding
import com.androidmacconnector.androidapp.devices.AddDeviceActivity
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
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
    }

    private fun setupButtonListeners() {
        binding.addRemoveDeviceBttn.setOnClickListener { view ->
            deviceRegistrationService.getDeviceId { deviceId, err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Error getting device id while checking if device is registered or not")
                    return@getDeviceId
                }

                // If it is unregistered, then register the device
                if (deviceId.isNullOrBlank()) {
                    startActivity(Intent(this, AddDeviceActivity::class.java))
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

        binding.signOutButton.setOnClickListener { view ->
            sessionStore.signOut()
            stopService(Intent(this, MQTTService::class.java))

            // Go to the sign in page
            startActivity(Intent(this, SignInActivity::class.java))
            finishAffinity()
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