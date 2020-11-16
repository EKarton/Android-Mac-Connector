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
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var sessionStore: SessionServiceImpl

    companion object {
        private const val LOG_TAG = "SignInActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)

        Log.d(LOG_TAG, "Create session store!")
        sessionStore = SessionServiceImpl(FirebaseAuth.getInstance())
        Log.d(LOG_TAG, "Created session store! $sessionStore")
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

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }
    }
}