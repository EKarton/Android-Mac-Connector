package com.androidmacconnector.androidapp.utils

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.androidmacconnector.androidapp.R
import org.json.JSONObject

open class WebService(private val context: Context) {
    companion object {
        private const val LOG_TAG = "WebService"
    }

    private val requestQueue: RequestQueue = VolleyRequestQueue.getInstance(context.applicationContext).requestQueue

    fun makeRequest(
        method: Int,
        url: String,
        jsonBody: JSONObject?,
        headers: Map<String, String>?,
        handler: WebServiceResponseHandler
    ) {
        Log.d(LOG_TAG, "Making HTTP request to $url with payload $jsonBody and header $headers")

        val request = object: JsonObjectRequest(method, url, jsonBody, handler, handler) {
            override fun getHeaders(): Map<String, String> {
                return headers ?: emptyMap()
            }
        }
        request.retryPolicy = DefaultRetryPolicy(
            500000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(request)
    }

    fun getServerProtocol(): String {
        return context.getString(R.string.protocol)
    }

    fun getServerAuthority(): String {
        return context.getString(R.string.authority)
    }

}

interface WebServiceResponseHandler : Response.Listener<JSONObject?>, Response.ErrorListener

class VolleyRequestQueue(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VolleyRequestQueue? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleyRequestQueue(context).also {
                    INSTANCE = it
                }
            }
    }

    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
}
