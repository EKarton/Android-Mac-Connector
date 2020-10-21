package com.androidmacconnector.androidapp.sms

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import com.androidmacconnector.androidapp.data.AndroidMacConnectorService
import com.androidmacconnector.androidapp.data.FcmSubscriber
import com.androidmacconnector.androidapp.data.UpdateSmsMessagesForThreadHandler
import com.androidmacconnector.androidapp.data.UpdateSmsThreadsHandler
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

interface GetSmsThreadsService {
    fun getSmsThreadsSummary(): List<SmsThreadSummary>
}

interface GetSmsMessagesFromThreadService {
    fun getSmsMessagesFromThread(threadId: String): List<MySmsMessage>
}

/**
 * A class used to handle all types of SMS-related tasks
 */
class SmsQueryService(private val contentResolver: ContentResolver) : GetSmsThreadsService,
    GetSmsMessagesFromThreadService {

    fun getRequiredPermissions(): List<String> {
        return arrayListOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS)
    }

    /**
     * Returns a list of SMS conversation threads.
     * This is useful when making an SMS app with the list of conversations on the left panel
     */
    override fun getSmsThreadsSummary(): List<SmsThreadSummary> {
        return this.getSmsConversations().map {
            val phoneNumber = getPhoneNumberFromThreadId(it.threadId)
            val lastMessageTime = getLastMessageTimeFromThreadId(it.threadId)
            val contactName: String? =
                if (phoneNumber != null) this.getContactNameFromPhoneNumber(phoneNumber) else null

            SmsThreadSummary(
                it.threadId,
                phoneNumber,
                contactName,
                0,
                it.numMessages,
                lastMessageTime!!,
                it.msgSnippet
            )
        }
    }

    private fun getSmsConversations(): List<SmsThread> {
        val cursor = contentResolver.query(
            Telephony.Sms.Conversations.CONTENT_URI,
            null,
            null,
            null,
            Telephony.Sms.Conversations.DEFAULT_SORT_ORDER
        );

        val smsConversations = arrayListOf<SmsThread>();

        if (cursor != null && cursor.moveToFirst()) {
            for (i in 0 until cursor.count) {
                val threadId = cursor.getString(cursor.getColumnIndexOrThrow("thread_id"))
                val numMessages = cursor.getString(cursor.getColumnIndexOrThrow("msg_count"))
                val msgSnippet = cursor.getString(cursor.getColumnIndexOrThrow("snippet"))

                val conversation = SmsThread(threadId, numMessages.toInt(), msgSnippet)
                smsConversations.add(conversation)

                cursor.moveToNext()
            }
        }

        cursor?.close()
        return smsConversations
    }

    private fun getPhoneNumberFromThreadId(threadId: String): String? {
        val projection = arrayOf("_id", "thread_id", "address", "person", "date", "body", "type");
        val selection = "thread_id = ?";
        val selectionArgs = arrayOf(threadId)
        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
        )

        var phoneNumber: String? = null;

        if (cursor != null && cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("address"))
        }

        cursor?.close()
        return phoneNumber
    }

    private fun getLastMessageTimeFromThreadId(threadId: String): Int? {
        val projection = arrayOf("_id", "thread_id", "address", "person", "date", "body", "type");
        val selection = "thread_id = ?";
        val selectionArgs = arrayOf(threadId)
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
        );

        var lastMessageTime: Int? = null;

        if (cursor != null && cursor.moveToFirst()) {
            val rawDate = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            val unixTimeInMs = rawDate.toLong()
            val unixTimeInSeconds = (unixTimeInMs / 1000).toInt()
            lastMessageTime = unixTimeInSeconds
        }

        cursor?.close()
        return lastMessageTime
    }

    private fun getContactNameFromPhoneNumber(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        val cursor = contentResolver.query(uri, projection, null, null, null)

        var contactName: String? = null;

        if (cursor != null && cursor.moveToFirst()) {
            contactName = cursor.getString(0);
        }
        cursor?.close()

        return contactName
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

class UpdateSmsThreadsRequestFcmSubscriber(
    private val smsQueryService: SmsQueryService,
    private val webService: AndroidMacConnectorService
) : FcmSubscriber {
    companion object {
        private const val LOG_TAG = "UpdateSmsThreads"
    }

    override fun getMessageAction(): String {
        return "update_sms_threads"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(LOG_TAG, "Message received: ${remoteMessage.data}")

        val threads = smsQueryService.getSmsThreadsSummary()
        webService.updateSmsThreads(threads, object : UpdateSmsThreadsHandler() {
            override fun onSuccess(response: JSONObject) {}
            override fun onError(exception: Exception?) {}
        })
    }
}

class UpdateSmsForThreadRequestFcmSubscriber(
    private val smsQueryService: SmsQueryService,
    private val webService: AndroidMacConnectorService
) : FcmSubscriber {
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

        webService.updateSmsMessagesForThread(
            threadId,
            messages,
            object : UpdateSmsMessagesForThreadHandler() {
                override fun onSuccess(response: JSONObject) {}
                override fun onError(exception: Exception?) {}
            })
    }
}

data class SmsThread(
    val threadId: String,
    val numMessages: Int,
    val msgSnippet: String
)

data class SmsThreadSummary(
    val threadId: String,
    val phoneNumber: String?,
    val contactName: String?,
    val numUnreadMessages: Int,
    val numMessages: Int,
    val lastMessageTime: Int,
    val lastMessage: String
)

data class MySmsMessage(
    val messageId: String,
    val address: String?,
    val person: String?,
    val body: String,
    val readState: Boolean,
    val time: Int,
    val type: String
)
