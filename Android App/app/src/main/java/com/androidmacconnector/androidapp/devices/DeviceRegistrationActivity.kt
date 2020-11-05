package com.androidmacconnector.androidapp.devices

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.MainActivity
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.sms.receiver.ReceivedSmsBroadcastReceiver
import com.androidmacconnector.androidapp.sms.messages.GetSmsMessagesServiceImpl
import com.androidmacconnector.androidapp.utils.getOrCreateUniqueDeviceId
import com.androidmacconnector.androidapp.utils.saveDeviceId
import com.google.firebase.auth.FirebaseAuth
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

/**
 * This activity is about registering this device to the server
 */
class DeviceRegistrationActivity : AppCompatActivity() {
    companion object {
        private const val LOG_TAG = "DeviceRegisterActivity"
        private val PERMISSIONS_TO_CAPABILITIES = mapOf(
            Manifest.permission.RECEIVE_SMS to "receive_sms",
            Manifest.permission.READ_SMS to "read_sms",
            Manifest.permission.READ_CONTACTS to "read_contacts",
            Manifest.permission.SEND_SMS to "send_sms"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_registration)
    }

    fun onYesButtonClickedHandler(view: View) {
        val requiredPermissions = GetSmsMessagesServiceImpl.getRequiredPermissions() +
                ReceivedSmsBroadcastReceiver.getRequiredPermissions() +
                listOf(Manifest.permission.SEND_SMS)

        val permsListener = object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                Log.d(LOG_TAG, "Permissions granted")
                onPermissionsGrantedHandler(report)
            }

            override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest?>?, token: PermissionToken?) {
                Log.d(LOG_TAG, "Permissions not granted")
                token?.continuePermissionRequest();
            }
        }

        Dexter.withContext(this)
            .withPermissions(requiredPermissions)
            .withListener(permsListener)
            .onSameThread()
            .check()
    }

    fun onNoButtonClickedHandler(view: View) {
        goToMainActivity()
    }

    private fun goToMainActivity() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

    private fun onPermissionsGrantedHandler(report: MultiplePermissionsReport) {
        val deviceId = getOrCreateUniqueDeviceId(this)
        val deviceService = DeviceWebService(this)

        // Get the device capabilities
        val capabilities = report.grantedPermissionResponses.filter {
            PERMISSIONS_TO_CAPABILITIES.containsKey(it.permissionName)
        }.map {
            PERMISSIONS_TO_CAPABILITIES[it.permissionName]!!
        }

        // Get the access token
        val context = this
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result?.token != null) {
                val accessToken = task.result?.token!!
                Log.d(LOG_TAG, "Access token: $accessToken")

                // Register the device
                deviceService.registerDevice(accessToken, deviceId, capabilities, object : RegisterDeviceHandler() {
                    override fun onSuccess(deviceId: String) {
                        saveDeviceId(context, deviceId)
                        goToMainActivity()
                    }

                    override fun onError(exception: Exception) {
                        throw exception
                    }
                })

            } else {
                throw task.exception ?: Exception("Cannot get access token")
            }
        }
    }
}