package com.androidmacconnector.androidapp.sms.messages

import android.Manifest
import android.content.ContentResolver
import android.provider.Telephony

data class SmsMessage(
    val messageId: String,
    val address: String?,
    val person: String?,
    val body: String,
    val readState: Boolean,
    val time: Int,
    val type: String
)

interface GetSmsMessagesService {
    fun getSmsMessagesFromThread(threadId: String, limit: Int, start: Int): List<SmsMessage>
}

/**
 * A class used to handle all types of SMS-related tasks
 */
class GetSmsMessagesServiceImpl(private val contentResolver: ContentResolver) : GetSmsMessagesService {
    companion object {
        fun getRequiredPermissions(): List<String> {
            return arrayListOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS)
        }
    }

    /**
     * Returns a list of SMS messages from a particular thread
     */
    override fun getSmsMessagesFromThread(threadId: String, limit: Int, start: Int): List<SmsMessage> {
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

        val smsMessages = arrayListOf<SmsMessage>()

        if (cursor != null && start < cursor.count && cursor.moveToPosition(start)) {
            val numElementsToFetch = Math.min(limit, cursor.count - start)

            for (i in 0 until numElementsToFetch) {
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
                    SmsMessage(messageId, address, person, body, readState, timeInSeconds, type)
                smsMessages.add(smsMessage)

                cursor.moveToNext()
            }
        }

        cursor?.close();
        return smsMessages
    }
}