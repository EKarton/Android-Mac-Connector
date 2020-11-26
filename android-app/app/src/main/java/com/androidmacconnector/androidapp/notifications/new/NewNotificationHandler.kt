package com.androidmacconnector.androidapp.notifications.new

abstract class NewNotificationHandler {

    private var next: NewNotificationHandler? = null

    fun setNext(next: NewNotificationHandler) {
        this.next = next
    }

    fun getNext(): NewNotificationHandler? {
        return this.next
    }

    fun handleNotification(notification: NewNotification) {
        if (onHandleNotification(notification)) {
            next?.handleNotification(notification)
        }
    }

    internal abstract fun onHandleNotification(notification: NewNotification): Boolean
}

