package com.androidmacconnector.androidapp.devices

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.databinding.ActivityDeviceDetailsBinding
import com.androidmacconnector.androidapp.mqtt.MqttService


class DeviceDetailsActivity : AppCompatActivity() {
    private lateinit var device: Device
    private lateinit var binding: ActivityDeviceDetailsBinding

    companion object {
        private const val LOG_TAG = "DeviceDetailsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the binding and the view
        binding = ActivityDeviceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get the device passed in
        device = intent.getSerializableExtra("device") as Device
        binding.device = device

        // Set the event handlers
        binding.actionListeners = object: DeviceActionsListener {
            override fun onClickPingDeviceHandler(view: View) {
                Log.d(LOG_TAG, "Ping device button clicked")
                pingDevice()
            }
        }
    }

    /**
     * Is called when the user clicks on the back button
     * It will go back to the previous activity
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun pingDevice() {
        val startIntent = Intent(this.applicationContext, MqttService::class.java)
        startIntent.action = MqttService.PUBLISH_INTENT_ACTION
        startIntent.putExtra("topic", "${device.deviceId}/ping/requests")
        startIntent.putExtra("payload", "")

        this.applicationContext.startService(startIntent)
    }
}

interface DeviceActionsListener {
    fun onClickPingDeviceHandler(view: View)
}