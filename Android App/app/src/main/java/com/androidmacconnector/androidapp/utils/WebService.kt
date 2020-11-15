package com.androidmacconnector.androidapp.utils

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.androidmacconnector.androidapp.R
import org.json.JSONObject

open class WebService(private val context: Context) {
    companion object {
        private const val LOG_TAG = "WebService"
    }

    private val requestQueue: RequestQueue = VolleyRequestQueue.getInstance(context.applicationContext).requestQueue

    fun makeStringRequest(
        method: Int,
        url: String,
        body: String?,
        headers: Map<String, String>?,
        handler: WebServiceStringResponseHandler
    ) {
        Log.d(LOG_TAG, "Making HTTP request to $url with payload $body and header $headers")

        val request = object: StringRequest(method, url, handler, handler) {
            override fun getBody(): ByteArray {
                return body?.toByteArray() ?: ByteArray(0)
            }
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

    fun makeJsonObjectRequest(
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

    fun makeJsonObjectRequest2(method: Int, url: String, jsonBody: JSONObject?, headers: Map<String, String>?, handler: (JSONObject?, Throwable?) -> Unit) {
        Log.d(LOG_TAG, "Making HTTP request2 to $url with payload $jsonBody and header $headers")

        val errorListener = Response.ErrorListener { error -> handler(null, error) }

        val responseHandler = Response.Listener<JSONObject?> { response -> handler(response, null) }

        val request = object: JsonObjectRequest(method, url, jsonBody, responseHandler, errorListener) {
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

interface WebServiceStringResponseHandler : Response.Listener<String?>, Response.ErrorListener

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
