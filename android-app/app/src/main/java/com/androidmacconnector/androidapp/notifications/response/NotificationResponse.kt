package com.androidmacconnector.androidapp.notifications.response

import org.json.JSONObject

class NotificationResponse(
    val key: String,
    val actionType: String,
    val actionTitle: String,
    val actionReplyMessage: String?
) {
    companion object {
        fun fromJsonObject(json: JSONObject): NotificationResponse {
            val key: String
            if (json.has("key")) {
                key = json.getString("key")
            } else {
                throw IllegalArgumentException("Missing key in json")
            }

            val actionType: String
            if (json.has("action_type")) {
                actionType = json.getString("action_type")
            } else {
                throw IllegalArgumentException("Missing action_type in json")
            }

            val actionTitle: String
            if (json.has("action_title")) {
                actionTitle = json.getString("action_title")
            } else {
                throw IllegalArgumentException("Missing action_title in json")
            }

            var actionReplyMessage: String? = null
            if (json.has("action_reply_message")) {
                actionReplyMessage = json.getString("action_reply_message")
            }

            return NotificationResponse(key, actionType, actionTitle, actionReplyMessage)
        }
    }
}