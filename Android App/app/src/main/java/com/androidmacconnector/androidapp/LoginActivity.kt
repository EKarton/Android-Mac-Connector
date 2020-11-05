package com.androidmacconnector.androidapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.devices.DeviceRegistrationActivity
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.devices.IsDeviceRegisteredHandler
import com.androidmacconnector.androidapp.utils.getOrCreateUniqueDeviceId
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val LOG_TAG = "LoginActivity"
        private const val RC_SIGN_IN = 343
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )

        val firebaseLoginActivity = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        startActivityForResult(firebaseLoginActivity, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (this.checkIfSignedIn(requestCode, resultCode)) {
            handleSigninSuccessful()
        }
    }

    private fun checkIfSignedIn(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                return true
            }
        }
        return false
    }

    private fun handleSigninSuccessful() {
        val deviceService = DeviceWebService(this)
        val deviceId = getOrCreateUniqueDeviceId(this)

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(false)?.addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result?.token == null) {
                throw Exception("Cannot get token")
            }
            val accessToken = task.result?.token!!

            Log.d(LOG_TAG, "Access token: $accessToken")

            // If the device is registered, then go to the main activity; else register the device
            deviceService.isDeviceRegistered(accessToken, deviceId, object : IsDeviceRegisteredHandler() {
                override fun onSuccess(isRegistered: Boolean) {
                    if (isRegistered) {
                        goToMainActivity()
                    } else {
                        goToDeviceRegistrationActivity()
                    }
                }

                override fun onError(exception: Exception) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    private fun goToDeviceRegistrationActivity() {
        val i = Intent(this, DeviceRegistrationActivity::class.java)
        startActivity(i)
    }

    private fun goToMainActivity() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }
}