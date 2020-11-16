package com.androidmacconnector.androidapp.auth

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.androidmacconnector.androidapp.R
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var sessionStore: SessionServiceImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        sessionStore = SessionServiceImpl(FirebaseAuth.getInstance())
    }

    fun onSignUpButtonClicked(view: View) {

    }
}