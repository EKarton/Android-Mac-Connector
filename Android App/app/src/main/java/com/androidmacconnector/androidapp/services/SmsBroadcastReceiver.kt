package com.androidmacconnector.androidapp.services

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import com.androidmacconnector.androidapp.data.AndroidMacConnectorServiceImpl
import com.androidmacconnector.androidapp.data.NotifyNewSmsMessageReceivedHandler
import com.androidmacconnector.androidapp.data.ReceivedSmsMessage
import org.json.JSONObject

/**
 * This class is responsible for receiving SMS messages from Android
 */
class SmsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val LOG_TAG = "SmsBroadcastReceiver"
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {

        // Get the SMS message.
        val bundle = intent.extras
        val format = bundle!!.getString("format")

        // Retrieve the SMS message received.
        val pdus = bundle["pdus"] as Array<*>?

        val smsMessages = arrayListOf<SmsMessage>()

        if (pdus != null) {
            for (i in pdus.indices) {
                val smsMessage = getSmsMessageFromPdu(pdus[i] as ByteArray, format)
                smsMessages.add(smsMessage)

                Log.d(LOG_TAG, "Received SMS message: $smsMessage")
            }
        }

        this.onReceiveSmsMessages(context, smsMessages)
    }

    private fun getSmsMessageFromPdu(pdu: ByteArray, format: String?): SmsMessage {
        // If Android version M or newer, use the appropriate createFromPdu function
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return SmsMessage.createFromPdu(pdu, format)
        } else {
            return SmsMessage.createFromPdu(pdu)
        }
    }

    private fun onReceiveSmsMessages(context: Context, smsMessages: List<SmsMessage>) {
        val webService = AndroidMacConnectorServiceImpl(context)

        // Get the contact info for each sms message
        smsMessages.forEach {
            val phoneNumber = it.displayOriginatingAddress
            val body = it.messageBody
            val timestamp = (it.timestampMillis / 1000).toInt()
            val sms = ReceivedSmsMessage(phoneNumber, body, timestamp)

            webService.notifyNewSmsMessageRecieved(sms, object : NotifyNewSmsMessageReceivedHandler() {
                override fun onSuccess(response: JSONObject) {}
                override fun onError(exception: Exception?) {}
            })
        }
    }
}

//
//class MySmsReceiver : BroadcastReceiver() {
//    /**
//     * Called when the BroadcastReceiver is receiving an Intent broadcast.
//     *
//     * @param context  The Context in which the receiver is running.
//     * @param intent   The Intent received.
//     */
//    @TargetApi(Build.VERSION_CODES.M)
//    override fun onReceive(context: Context, intent: Intent) {
//        // Get the SMS message.
//        val bundle = intent.extras
//        val msgs: Array<SmsMessage?>
//        var strMessage = ""
//        val format = bundle!!.getString("format")
//        // Retrieve the SMS message received.
//        val pdus = bundle[pdu_type] as Array<Any>?
//        if (pdus != null) {
//            // Check the Android version.
//            val isVersionM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//            // Fill the msgs array.
//            msgs = arrayOfNulls(pdus.size)
//            for (i in msgs.indices) {
//                // Check Android version and use appropriate createFromPdu.
//                if (isVersionM) {
//                    // If Android version M or newer:
//                    msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
//                } else {
//                    // If Android version L or older:
//                    msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
//                }
//                // Build the message to show.
//                strMessage += "SMS from " + msgs[i]?.getOriginatingAddress()
//                strMessage += """ :${msgs[i]?.messageBody)}"""
//                // Log and display the SMS message.
//                Log.d(TAG, "onReceive: $strMessage")
//                Toast.makeText(context, strMessage, Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    companion object {
//        private val TAG = MySmsReceiver::class.java.simpleName
//        const val pdu_type = "pdus"
//    }
//}

