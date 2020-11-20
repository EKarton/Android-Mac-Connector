package com.androidmacconnector.androidapp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.androidmacconnector.androidapp.MainActivity
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.databinding.ActivitySignInBinding
import com.androidmacconnector.androidapp.devices.AddDeviceActivity
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var sessionStore: SessionStoreImpl
    private lateinit var deviceRegistrationService: DeviceRegistrationService

    companion object {
        private const val LOG_TAG = "SignInActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)

        val deviceService = DeviceWebServiceImpl(this)
        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        deviceRegistrationService = DeviceRegistrationService(this, sessionStore, deviceService)
    }

    /** Called when the user clicks on the Create Account button **/
    fun onCreateAccountButtonClicked(view: View) {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    /** Called when the user clicks on the Sign In button **/
    fun onSignInButtonClicked(view: View) {
        val email = binding.email
        val password = binding.password

        if (email.isNullOrBlank()) {
            Log.d(LOG_TAG, "Error logging in: missing email")
            binding.errorMessage = "Email should not be blank"
            return
        }

        if (password.isNullOrBlank()) {
            Log.d(LOG_TAG, "Error logging in: missing password")
            binding.errorMessage = "Password should not be blank"
            return
        }

        sessionStore.signIn(email, password) { err ->
            if (err != null) {
                Log.d(LOG_TAG, "Error logging in: $err")
                binding.errorMessage = err.localizedMessage
                return@signIn
            }

            Log.d(LOG_TAG, "Successfully logged in")
            handleOnSigninSuccessful()
        }
    }

    private fun handleOnSigninSuccessful() {
        deviceRegistrationService.getDeviceId { deviceId, err ->
            if (err != null) {
                Log.d(LOG_TAG, "Error checking if it is registered or not: $err")
                return@getDeviceId
            }

            Intent(this, MQTTService::class.java).also {
                stopService(it)
                startService(it)
            }

            startActivity(Intent(this, MainActivity::class.java))

            if (deviceId.isNullOrBlank()) {
                startActivity(Intent(this, AddDeviceActivity::class.java))
            }

            finish()
        }
    }
}