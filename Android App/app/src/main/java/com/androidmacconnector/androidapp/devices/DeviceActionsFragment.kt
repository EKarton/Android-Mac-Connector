package com.androidmacconnector.androidapp.devices

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.auth.SessionStoreImpl
import com.androidmacconnector.androidapp.databinding.FragmentDeviceActionsBinding
import com.androidmacconnector.androidapp.mqtt.MQTTService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class DeviceActionsFragment : Fragment() {

    private var devices = listOf<Device>()
    private var dataBindings: FragmentDeviceActionsBinding? = null

    companion object {
        private const val LOG_TAG = "DeviceActionsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = this.requireContext()

        SessionStoreImpl(FirebaseAuth.getInstance()).getAuthToken { authToken, err ->
            if (err != null) {
                Log.d(LOG_TAG, "Error when getting auth token: $err")
                return@getAuthToken
            }

            DeviceWebService(context).getDevices(authToken) { newDevices, err2 ->
                if (err2 != null) {
                    Log.d(LOG_TAG, "Error when getting devices: $err2")
                    return@getDevices
                }

                val pingableDevices = newDevices.filter { device ->
                    device.canPingDevice()
                }

                devices = newDevices
                dataBindings?.hasPingableDevice = pingableDevices.count() > 0
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_device_actions, container, false)

        // Bind the bindings to this view
        dataBindings = FragmentDeviceActionsBinding.bind(view)

        dataBindings?.listeners = object: DeviceActionsListener {
            override fun onClickPingDeviceHandler(view: View) {
                Log.d(LOG_TAG, "Ping device button clicked")
                showPingDeviceDialog()
            }
        }

        return view
    }

    private fun showPingDeviceDialog() {
        val deviceNames = devices.map { device -> device.name }.toTypedArray()
        val checkedItems = devices.map { false }.toTypedArray().toBooleanArray()
        val selectedDevices = hashSetOf<Int>()

        MaterialAlertDialogBuilder(context)
            .setTitle("Ping devices?")
            .setMultiChoiceItems(deviceNames, checkedItems) { _, which, checked ->
                Log.d(LOG_TAG, "Chose $which $checked")
                if (checked) {
                    selectedDevices.add(which)
                } else {
                    selectedDevices.remove(which)
                }
            }
            .setNeutralButton("Cancel") { dialog, which -> }
            .setPositiveButton("Ping selected devices") { _, _ ->
                pingDevices(devices.filterIndexed{ i, _ -> selectedDevices.contains(i) })
            }
            .show()
    }

    private fun pingDevices(devices: List<Device>) {
        Log.d(LOG_TAG, "Pinging devices: $devices")

        devices.forEach { device ->
            val startIntent = Intent(this.context, MQTTService::class.java)
            startIntent.action = MQTTService.PUBLISH_INTENT_ACTION
            startIntent.putExtra("topic", "${device.deviceId}/ping/requests")
            startIntent.putExtra("payload", "")

            context?.startService(startIntent)
        }
    }
}