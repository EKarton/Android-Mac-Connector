package com.androidmacconnector.androidapp.sms

import android.Manifest
import android.content.ContentResolver
import android.provider.Telephony
import android.util.Log
import com.androidmacconnector.androidapp.fcm.FcmSubscriber
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

interface GetSmsMessagesFromThreadService {
    fun getSmsMessagesFromThread(threadId: String): List<MySmsMessage>
}

/**
 * A class used to handle all types of SMS-related tasks
 */
class SmsQueryService(private val contentResolver: ContentResolver) : GetSmsMessagesFromThreadService {
    companion object {
        fun getRequiredPermissions(): List<String> {
            return arrayListOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS)
        }
    }

    /**
     * Returns a list of SMS messages from a particular thread
     */
    override fun getSmsMessagesFromThread(threadId: String): List<MySmsMessage> {
        val projection =
            arrayOf("_id", "address", "person", "date", "body", "read", "date", "type");
        val selection = "thread_id = ?";
        val selectionArgs = arrayOf(threadId)
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        val smsMessages = arrayListOf<MySmsMessage>();

        if (cursor != null && cursor.moveToFirst()) {
            for (i in 0 until cursor.count) {
                val messageId = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                val person = cursor.getString(cursor.getColumnIndexOrThrow("person"))
                val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                val readState = cursor.getString(cursor.getColumnIndexOrThrow("read")) == "1"

                val timeInMs = cursor.getString(cursor.getColumnIndexOrThrow("date")).toLong()
                val timeInSeconds = (timeInMs / 1000).toInt()

                var type = "sent";
                if (cursor.getString(cursor.getColumnIndexOrThrow("type")).contains("1")) {
                    type = "inbox";
                }

                val smsMessage =
                    MySmsMessage(messageId, address, person, body, readState, timeInSeconds, type)
                smsMessages.add(smsMessage)

                cursor.moveToNext()
            }
        }

        cursor?.close();
        return smsMessages
    }
}

class UpdateSmsForThreadRequestFcmSubscriber(private val smsQueryService: SmsQueryService, private val webService: SmsService) : FcmSubscriber {
    companion object {
        private const val LOG_TAG = "UpdateSmsForThread"
    }

    override fun getMessageAction(): String {
        return "update_sms_thread_messages"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "Message received: ${remoteMessage.data}")

        if (remoteMessage.data["thread_id"].isNullOrEmpty()) {
            Log.e(LOG_TAG, "Empty thread_id")
            return
        }

        val threadId = remoteMessage.data["thread_id"]!!
        val messages = smsQueryService.getSmsMessagesFromThread(threadId)

        webService.updateSmsMessagesForThread(threadId, messages, object : UpdateSmsMessagesForThreadHandler() {
            override fun onSuccess(response: JSONObject) {}
            override fun onError(exception: Exception?) {}
        })
    }
}

data class MySmsMessage(
    val messageId: String,
    val address: String?,
    val person: String?,
    val body: String,
    val readState: Boolean,
    val time: Int,
    val type: String
)
