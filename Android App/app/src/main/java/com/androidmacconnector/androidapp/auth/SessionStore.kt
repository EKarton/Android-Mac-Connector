package com.androidmacconnector.androidapp.auth

import com.google.firebase.auth.FirebaseAuth

interface SessionStore {
    fun getAuthToken(handler: (String, Exception?) -> Unit)
    fun isSignedIn(): Boolean
    fun signUp(email: String, password: String, handler: (Exception?) -> Unit)
    fun signIn(email: String, password: String, handler: (Exception?) -> Unit)
    fun signOut()
}

class SessionStoreImpl(private val firebaseAuth: FirebaseAuth): SessionStore {
    override fun getAuthToken(handler: (String, Exception?) -> Unit) {
        val user = firebaseAuth.currentUser
        user?.getIdToken(false)?.addOnCompleteListener { task ->

            if (task.exception != null) {
                handler("", task.exception)
                return@addOnCompleteListener
            }

            if (!task.isSuccessful || task.result?.token == null) {
                handler("", Exception("Failed to get access token"))
                return@addOnCompleteListener
            }

            val accessToken = task.result!!.token!!
            handler(accessToken, null)
        }
    }

    override fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun signUp(email: String, password: String, handler: (Exception?) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    handler(task.exception)
                    return@addOnCompleteListener
                }

                handler(null)
            }
    }

    override fun signIn(email: String, password: String, handler: (Exception?) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    handler(task.exception)
                    return@addOnCompleteListener
                }

                handler(null)
            }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}