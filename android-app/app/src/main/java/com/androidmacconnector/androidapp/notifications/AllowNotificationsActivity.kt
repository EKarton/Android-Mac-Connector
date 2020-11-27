package com.androidmacconnector.androidapp.notifications

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.auth.SessionStore
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.devices.DeviceRegistrationService
import com.androidmacconnector.androidapp.devices.DeviceWebService
import com.androidmacconnector.androidapp.devices.DeviceWebServiceImpl
import com.androidmacconnector.androidapp.devices.UpdatedDevice
import com.google.firebase.auth.FirebaseAuth

/**
 * An activity that asks permission to listen to notifications
 */
class AllowNotificationsActivity : AppCompatActivity() {

    companion object {
        private const val LOG_TAG = "AllowNotifyActivity"
    }

    private lateinit var sessionStore: SessionStore
    private lateinit var deviceWebService: DeviceWebService
    private lateinit var deviceRegistrationService: DeviceRegistrationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allow_notifications)

        sessionStore = SessionStoreImpl(FirebaseAuth.getInstance())
        deviceWebService = DeviceWebServiceImpl(this)
        deviceRegistrationService = DeviceRegistrationService(this, sessionStore, deviceWebService)
    }

    /** Called when the allow button was clicked */
    fun onAllowButtonClicked(view: View) {
        if (!checkIfPermissionGranted()) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivityForResult(intent, 1)

        } else {
            Log.d(LOG_TAG, "Already has permissions granted")
            addPermissionToDeviceCapabilities { err ->
                if (err != null) {
                    Log.d(LOG_TAG, "Failed to add permission to device capabilities: $err")
                }
                finish()
            }
        }
    }

    /** This is called when the settings activity is returned back to the user */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(LOG_TAG, "Returned from settings activity")

        if (requestCode == 1) {
            if (checkIfPermissionGranted()) {
                addPermissionToDeviceCapabilities { err ->
                    if (err != null) {
                        Log.d(LOG_TAG, "Failed to add permission to device capabilities: $err")
                    }
                    finish()
                }

            } else {
                finish()
            }
        }
    }

    private fun checkIfPermissionGranted(): Boolean {
        if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners") != null) {
            if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners").contains(this.packageName)) {
                return true
            }
        }
        return false
    }

    private fun addPermissionToDeviceCapabilities(handler: (Throwable?) -> Unit) {
        sessionStore.getAuthToken { authToken, err ->
            if (err != null) {
                handler(err)
                return@getAuthToken
            }

            deviceRegistrationService.getDeviceId { deviceId, err2 ->
                if (err2 != null) {
                    handler(err2)
                    return@getDeviceId
                }

                if (deviceId.isNullOrBlank()) {
                    handler(IllegalStateException("Device id is blank"))
                    return@getDeviceId
                }

                deviceWebService.getDevice(authToken, deviceId) { device, err3 ->
                    if (err3 != null) {
                        handler(err3)
                        return@getDevice
                    }

                    if (device == null) {
                        handler(IllegalStateException("Device is null"))
                        return@getDevice
                    }

                    val updatedCapabilities = device.capabilities.toMutableList()
                    updatedCapabilities.add("receive_notifications")
                    updatedCapabilities.add("respond_to_notifications")

                    val updatedProperties = UpdatedDevice(null, null, updatedCapabilities)

                    deviceWebService.updateDevice(authToken, deviceId, updatedProperties) { err4 ->
                        if (err4 != null) {
                            handler(err4)
                            return@updateDevice
                        }

                        handler(null)
                    }
                }
            }
        }
    }

    /** Called when the No Thanks button was clicked */
    fun onCancelButtonClicked(view: View) {
        finish()
    }
}