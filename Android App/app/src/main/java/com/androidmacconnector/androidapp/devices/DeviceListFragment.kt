package com.androidmacconnector.androidapp.devices

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.androidmacconnector.androidapp.databinding.FragmentDeviceListBinding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidmacconnector.androidapp.R
import com.androidmacconnector.androidapp.auth.SessionServiceImpl
import com.androidmacconnector.androidapp.utils.getDeviceIdSafely

class DeviceListFragment: Fragment() {

    companion object {
        private const val LOG_TAG = "DeviceListFragment"
    }

    private var dataBindings: FragmentDeviceListBinding? = null
    private var devices: MutableList<Device> = mutableListOf()
    private var adapter: DeviceListAdapter? = null

    /**
     * Called when creating the fragment
     * Will fetch the number of devices listed in this account
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = this.requireContext()
        val curDeviceId = getDeviceIdSafely(context) ?: throw Exception("DeviceID should be set here!")

        SessionServiceImpl().getAuthToken { authToken, err ->
            if (authToken.isNullOrBlank() || err != null) {
                Log.d(LOG_TAG, "Missing auth: $err")
                return@getAuthToken
            }

            DeviceWebService(context).getDevices(authToken) { newDevices, err2 ->
                if (err2 != null) {
                    Log.d(LOG_TAG, "Error when getting devices: $err2")
                    return@getDevices
                }

                // Remove our current device from the list
                val filteredDevices = newDevices.filter { it.deviceId != curDeviceId }

                devices.clear()
                devices.addAll(filteredDevices)
                adapter?.notifyDataSetChanged()

                dataBindings?.showNoDevicesPrompt = devices.count() == 0
            }
        }
    }

    /**
     * Called when it's time for the fragment to draw its user interface for the first time
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Create the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_device_list, container, false)

        // Set the RecyclerView to the list of devices
        val devicesList = view.findViewById(R.id.devicesRecyclerView) as RecyclerView
        adapter = DeviceListAdapter(devices)
        devicesList.adapter = adapter
        devicesList.layoutManager = LinearLayoutManager(this.context)

        // Set up data bindings
        dataBindings = FragmentDeviceListBinding.inflate(layoutInflater, container, false)

        return view
    }
}

class DeviceListAdapter(private val devices: List<Device>) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    // Represents a row's view in the adapter
    inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        val deviceImage = itemView.findViewById(R.id.deviceImage) as ImageView
        val deviceNameLabel = itemView.findViewById(R.id.deviceNameLabel) as TextView
    }

    // Creates a view for a row in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val contactView = inflater.inflate(R.layout.fragment_device_list_row, parent, false)
        return ViewHolder(contactView)
    }

    // Populates the data into an item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceNameLabel.text = device.name

        val imageResource = when(device.type) {
            "macbook" -> R.drawable.ic_macbook
            "iphone" -> R.drawable.ic_iphone
            "ipad" -> R.drawable.ic_ipad
            "imac" -> R.drawable.ic_imac
            "android_phone" -> R.drawable.ic_android_phone
            else -> R.drawable.ic_generic_device
        }

        holder.deviceImage.setImageResource(imageResource)
    }

    override fun getItemCount(): Int {
        return devices.count()
    }
}