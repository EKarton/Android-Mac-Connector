package com.androidmacconnector.androidapp.auth

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.androidmacconnector.androidapp.MainActivity
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.databinding.ActivitySignInBinding
import com.androidmacconnector.androidapp.devices.AddDeviceActivity
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var sessionStore: SessionStoreImpl
    private lateinit var deviceService: DeviceWebService

    companion object {
        private const val LOG_TAG = "SignInActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)

        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        deviceService = DeviceWebService(this)
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
        val hardwareId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID);

        sessionStore.getAuthToken { authToken, err ->
            if (err != null) {
                Log.d(LOG_TAG, "Failed to get auth token: $err")
                binding.errorMessage = "Error 1 - Please try again"
                return@getAuthToken
            }

            deviceService.isDeviceRegistered(authToken, "android_phone", hardwareId) { isRegistered, err2 ->
                if (err2 != null) {
                    Log.d(LOG_TAG, "Failed to check if device is registered: $err2")
                    binding.errorMessage = "Error 3 - Please try again"
                    return@isDeviceRegistered
                }

                if (isRegistered == null) {
                    Log.d(LOG_TAG, "isRegistered is null")
                    binding.errorMessage = "Error 4 - Please try again"
                    return@isDeviceRegistered
                }

                if (isRegistered) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
                    startActivity(intent)

                } else {
                    startActivity(Intent(this, AddDeviceActivity::class.java))
                }
            }
        }
    }
}