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
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    private lateinit var sessionStore: SessionStoreImpl
    private lateinit var deviceService: DeviceWebService
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
        deviceService = DeviceWebService(this)

        setupBackButton()
        setupSettingsContent()
        setupButtonListeners()
    }

    private fun setupBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupSettingsContent() {
        binding.isRegistered = isDeviceRegistered()

        sessionStore.getUserDetails().let { user ->
            binding.accountName = user?.email
        }
    }

    private fun setupButtonListeners() {
        binding.addRemoveDeviceBttn.setOnClickListener { view ->
            if (isDeviceRegistered()) {
                removeDevice()
            } else {
                registerDevice()
            }
        }

        binding.signOutButton.setOnClickListener { view -> signOut() }
    }

    private fun isDeviceRegistered(): Boolean {
        return getDeviceIdSafely(this) != null
    }

    private fun registerDevice() {
        startActivity(Intent(this, AddDeviceActivity::class.java))
    }

    private fun removeDevice() {
        sessionStore.getAuthToken { authToken, err1 ->
            if (err1 != null) {
                Log.d(LOG_TAG, "Error getting auth token: $err1")
                Toast.makeText(this, "Error unregistering device", Toast.LENGTH_LONG).show()
                return@getAuthToken
            }

            val deviceId = getDeviceIdSafely(this) ?: throw IllegalStateException("Device id should be present here")
            deviceService.unregisterDevice(authToken, deviceId) { err2 ->
                if (err2 != null) {
                    Log.d(LOG_TAG, "Error unregistering device: $err2")
                    Toast.makeText(this, "Error unregistering device", Toast.LENGTH_LONG).show()
                    return@unregisterDevice
                }

                Log.d(LOG_TAG, "Successfully unregistered device")
                binding.isRegistered = false
            }
        }
    }

    private fun signOut() {
        sessionStore.signOut()

        // Go to the sign in page
        val intent = Intent(this, SignInActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
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