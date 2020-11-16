package com.androidmacconnector.androidapp.utils

import android.content.Context
import com.androidmacconnector.androidapp.R

fun saveDeviceId(context: Context, deviceId: String) {
    val fileName = context.getString(R.string.app_data_file_key)
    val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    val key = context.getString(R.string.android_device_id)

    with (sharedPreferences.edit()) {
        putString(key, deviceId)
        apply()
    }
}

fun getDeviceIdSafely(context: Context): String? {
    val fileName = context.getString(R.string.app_data_file_key)
    val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    val key = context.getString(R.string.android_device_id)

    if (sharedPreferences.contains(key)) {
        return sharedPreferences.getString(key, null)
    }
    return null
}

@Deprecated("This is dangerous", ReplaceWith("getDeviceIdSafely(context: Context)"), DeprecationLevel.WARNING)
fun getDeviceId(context: Context): String {
    val fileName = context.getString(R.string.app_data_file_key)
    val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    val key = context.getString(R.string.android_device_id)

    if (sharedPreferences.contains(key)) {
        return sharedPreferences.getString(key, null) ?: throw Exception("Cannot get device token")
    }
    throw Exception("There is no device id")
}