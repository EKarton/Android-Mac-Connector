package com.androidmacconnector.androidapp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.androidmacconnector.androidapp.MainActivity
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.databinding.ActivitySignUpBinding
import com.androidmacconnector.androidapp.devices.AddDeviceActivity
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var sessionStore: SessionStoreImpl

    companion object {
        private const val LOG_TAG = "SignUpActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)

        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
    }

    fun onSignInButtonClicked(view: View) {
        onBackPressed()
    }

    fun onSignUpButtonClicked(view: View) {
        val email = binding.email
        val password1 = binding.password1
        val password2 = binding.password2

        if (email.isNullOrBlank()) {
            binding.errorMessage = "Email must not be blank"
            return
        }

        if (password1.isNullOrBlank()) {
            binding.errorMessage = "Password must not be blank"
            return
        }

        if (password1 != password2) {
            binding.errorMessage = "Passwords do not match"
            return
        }

        sessionStore.signUp(email, password1) { err ->
            if (err != null) {
                Log.d(LOG_TAG, "Error trying to sign up: $err")
                binding.errorMessage = err.localizedMessage
                return@signUp
            }

            Intent(this, MQTTService::class.java).also {
                stopService(it)
                startService(it)
            }

            startActivity(Intent(this, MainActivity::class.java))
            startActivity(Intent(this, AddDeviceActivity::class.java))
            finish()
        }
    }
}