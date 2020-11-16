package com.androidmacconnector.androidapp.devices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.MainActivity
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.ping.PingDeviceServiceImpl
import com.androidmacconnector.androidapp.sms.messages.ReadSmsMessagesReceiver
import com.androidmacconnector.androidapp.sms.receiver.ReceivedSmsReceiver
import com.androidmacconnector.androidapp.sms.sender.SendSmsReceiver
import com.androidmacconnector.androidapp.sms.threads.ReadSmsThreadsReceiver
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
class AddDeviceActivity : AppCompatActivity() {
    private lateinit var sessionStore: SessionStoreImpl
    private lateinit var deviceService: DeviceWebService

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

        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        deviceService = DeviceWebService(this)
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
        goToMainActivity()
    }

    private fun onPermissionsGrantedHandler(report: MultiplePermissionsReport) {
        val capabilities = getCapabilities(report)

        // Set up the ping device notifications channel
        if (capabilities.contains("ping_device")) {
            PingDeviceServiceImpl(this).setupNotificationChannel()
        }

        // Get the hardware id
        val hardwareId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)

        // Get the access token
        val context = this
        sessionStore.getAuthToken { authToken, err ->
            if (err != null) {
                Log.d(LOG_TAG, "Error getting auth token: $err")
                return@getAuthToken
            }

            deviceService.registerDevice(authToken, "android_phone", hardwareId, capabilities) { deviceId, err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Failed to register device, $err")
                    return@registerDevice
                }

                if (deviceId.isNullOrBlank()) {
                    Log.d(LOG_TAG, "Device ID is blank")
                    return@registerDevice
                }

                saveDeviceId(context, deviceId)
                goToMainActivity()
            }
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

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
    }
}