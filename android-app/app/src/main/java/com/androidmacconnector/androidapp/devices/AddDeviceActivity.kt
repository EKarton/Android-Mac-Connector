package com.androidmacconnector.androidapp.devices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.androidmacconnector.androidapp.sms.messages.ReadSmsMessagesReceiver
import com.androidmacconnector.androidapp.sms.receiver.ReceivedSmsReceiver
import com.androidmacconnector.androidapp.sms.sender.SendSmsReceiver
import com.androidmacconnector.androidapp.sms.threads.ReadSmsThreadsReceiver
import com.google.firebase.auth.FirebaseAuth
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

/**
 * This activity is about registering this device to the server
 */
class AddDeviceActivity : AppCompatActivity() {
    private lateinit var deviceRegistrationService: DeviceRegistrationService

    companion object {
        private const val LOG_TAG = "AddDeviceActivity"
        private val PERMISSIONS_TO_CAPABILITIES = mapOf(
            Manifest.permission.RECEIVE_SMS to "receive_sms",
            Manifest.permission.READ_SMS to "read_sms",
            Manifest.permission.READ_CONTACTS to "read_contacts",
            Manifest.permission.SEND_SMS to "send_sms"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        val sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        val deviceWebService = DeviceWebServiceImpl(this)
        deviceRegistrationService = DeviceRegistrationService(this, sessionStore, deviceWebService)
    }

    /** Called when the user clicks on the Register Device button **/
    fun onAddDeviceButtonClicked(view: View) {
        val permissions = hashSetOf<String>()

        // Add SMS permissions if there is SMS functionality on this device
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            permissions.addAll(ReceivedSmsReceiver.getRequiredPermissions())
            permissions.addAll(SendSmsReceiver.getRequiredPermissions())
            permissions.addAll(ReadSmsMessagesReceiver.getRequiredPermissions())
            permissions.addAll(ReadSmsThreadsReceiver.getRequiredPermissions())
        }

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
            .withPermissions(permissions)
            .withListener(permsListener)
            .check()
    }

    /** Called when the user clicks on the 'No Thanks' button **/
    fun onCancelButtonClicked(view: View) {
        finish()
    }

    private fun onPermissionsGrantedHandler(report: MultiplePermissionsReport) {
        val capabilities = getCapabilities(report)

        deviceRegistrationService.registerDevice(capabilities) { err ->
            if (err != null) {
                Toast.makeText(this, "Failed to register device", Toast.LENGTH_LONG).show()
                return@registerDevice
            }

            Toast.makeText(this, "Successfully registered device", Toast.LENGTH_LONG).show()
            Intent(this, MQTTService::class.java).also {
                stopService(it)
                startService(it)
            }
            finish()
        }
    }

    private fun getCapabilities(report: MultiplePermissionsReport): List<String> {
        // Get the device capabilities
        val capabilities = report.grantedPermissionResponses.filter {
            PERMISSIONS_TO_CAPABILITIES.containsKey(it.permissionName)
        }.map {
            PERMISSIONS_TO_CAPABILITIES[it.permissionName]!!
        }

        val newCapabilities = mutableListOf<String>()
        newCapabilities.addAll(capabilities)
        newCapabilities.add("ping_device")

        return newCapabilities
    }
}