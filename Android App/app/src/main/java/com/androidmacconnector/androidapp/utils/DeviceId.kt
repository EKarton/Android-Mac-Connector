package com.androidmacconnector.androidapp.utils

import android.content.Context

import com.androidmacconnector.androidapp.R
import java.util.*

fun getOrCreateUniqueDeviceId(context: Context): String {
    val fileName = context.getString(R.string.app_data_file_key)
    val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    val key = context.getString(R.string.android_device_id)

    if (sharedPreferences.contains(key)) {
        return sharedPreferences.getString(key, null) ?: throw Exception("Cannot get device token")
    }

    val uniqueID: String = UUID.randomUUID().toString()

    with (sharedPreferences.edit()) {
        putString(key, uniqueID)
        apply()
    }

    return uniqueID
}