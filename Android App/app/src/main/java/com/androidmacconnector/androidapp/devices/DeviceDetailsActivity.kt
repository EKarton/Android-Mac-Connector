package com.androidmacconnector.androidapp.devices

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.databinding.ActivityDeviceDetailsBinding


class DeviceDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceDetailsBinding

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
        val device = intent.getSerializableExtra("device") as Device
        binding.device = device
    }

    /**
     * Is called when the user clicks on the back button
     * It will go back to the previous activity
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}