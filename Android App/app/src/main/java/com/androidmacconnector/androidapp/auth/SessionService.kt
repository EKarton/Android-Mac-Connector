package com.androidmacconnector.androidapp.auth

import com.google.firebase.auth.FirebaseAuth

interface SessionService {
    fun getAuthToken(handler: (String?, Exception?) -> Unit)
}

class SessionServiceImpl: SessionService {
    override fun getAuthToken(handler: (String?, Exception?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(false)?.addOnCompleteListener { task ->

            if (task.exception != null) {
                handler(null, task.exception)
                return@addOnCompleteListener
            }

            if (!task.isSuccessful || task.result?.token == null) {
                handler(null, Exception("Failed to get access token"))
                return@addOnCompleteListener
            }

            val accessToken = task.result!!.token!!
            handler(accessToken, null)
        }
    }
}