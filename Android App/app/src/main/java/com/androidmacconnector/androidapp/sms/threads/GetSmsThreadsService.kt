package com.androidmacconnector.androidapp.sms.threads

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony

interface GetSmsThreadsService {
    fun getSmsThreadsSummary(contentResolver: ContentResolver, limit: Int, start: Int): List<SmsThreadSummary>
}

class GetSmsThreadsServiceImpl: GetSmsThreadsService {
    /**
     * Returns a list of SMS conversation threads.
     * This is useful when making an SMS app with the list of conversations on the left panel
     */
    override fun getSmsThreadsSummary(contentResolver: ContentResolver, limit: Int, start: Int): List<SmsThreadSummary> {
        return this.getSmsConversations(contentResolver, limit, start).map {
            val phoneNumber = getPhoneNumberFromThreadId(contentResolver, it.threadId)
            val numUnreadMessages = getNumUnreadMessagesFromThreadId(contentResolver, it.threadId)
            val lastMessageTime = getTimeLastMessageSent(contentResolver, it.threadId)

            var contactName: String? = null
            if (phoneNumber != null) {
                contactName = this.getContactNameFromPhoneNumber(contentResolver, phoneNumber)
            }

            SmsThreadSummary(
                it.threadId,
                phoneNumber,
                contactName,
                numUnreadMessages ?: 0,
                it.numMessages,
                lastMessageTime!!,
                it.msgSnippet
            )
        }
    }

    private fun getSmsConversations(contentResolver: ContentResolver, limit: Int, start: Int): List<SmsThread> {
        val cursor = contentResolver.query(
            Telephony.Sms.Conversations.CONTENT_URI,
            null,
            null,
            null,
            Telephony.Sms.Conversations.DEFAULT_SORT_ORDER
        )

        val smsConversations = arrayListOf<SmsThread>()

        if (cursor != null && start < cursor.count && cursor.moveToPosition(start)) {
            val numElementsToFetch = Math.min(limit, cursor.count - start)

            for (i in 0 until numElementsToFetch) {
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

    private fun getPhoneNumberFromThreadId(contentResolver: ContentResolver, threadId: String): String? {
        val projection = arrayOf(Telephony.TextBasedSmsColumns.ADDRESS);
        val selection = "thread_id = ?";
        val selectionArgs = arrayOf(threadId)
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        var phoneNumber: String? = null;

        if (cursor != null && cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("address"))
        }

        cursor?.close()
        return phoneNumber
    }

    private fun getNumUnreadMessagesFromThreadId(contentResolver: ContentResolver, threadId: String): Int? {
        val selection = "read = ? and thread_id = ?";
        val selectionArgs = arrayOf("0", threadId)
        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        val numUnreadMessages = cursor?.count

        cursor?.close()

        return numUnreadMessages
    }

    private fun getTimeLastMessageSent(contentResolver: ContentResolver, threadId: String): Int? {
        val projection = arrayOf(Telephony.TextBasedSmsColumns.DATE);
        val selection = "${Telephony.TextBasedSmsColumns.THREAD_ID} = ?";
        val selectionArgs = arrayOf(threadId)
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.DEFAULT_SORT_ORDER
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

    private fun getContactNameFromPhoneNumber(contentResolver: ContentResolver, phoneNumber: String): String? {
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