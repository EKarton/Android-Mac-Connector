# Introduction

This document is about how messages get sent back and forth to achieve the required features ([link](Design\ Document.md)). These features can be broken down into smaller tasks:

1. Receive sms text from my Android phone to my Mac

2. Send sms text from my Mac to my Android phone

3. Getting a list of SMS conversations on the Android device from the Mac device

4. When getting SMS messages of a particular thread on an Android device in the Mac device:

# Background information

### FCM:

With FCM, you cna send two types of messages asynchronously to Android devices:

* Notification messages: are messages that get alerted to the user
* Data messages: are messages that don't get alerted to the user

Notification message format:

``` 
{
  "message": {
    "token": "<TOKEN>",
    "notification": {
      "title": "<TITLE OF NOTIFICATION>",
      "body": "<BODY TEXT>"
    },
    "data": {
      ...
    }
  }
}
```

Data message format:

``` 
{
  "message": {a
    "token": "<TOKEN>",
    "data": {
      ...
    }
  }
}
```

 * Note:
   * For each message sent out, make sure that they do not have any of these keys: "from", "notification", "message_type", or any key that starts with "google" or "gcm" ([link](https://firebase.google.com/docs/cloud-messaging/concept-options#data_messages))
   * The data values can only be strings (no recursive objects)


# Solutions to achieve desired tasks:

### Registration

1. When the Android device wants to register to the service, it sends a request to the server (**POST** ```api/v1/device-registration```)

    ``` 
    {
      "device_type": "<device type>",
      ... (more data)
    }
    ```

    Specifically, if the device is an Android device, send in the FCM device token:

    ``` 
    {
      "device_type": "android",
      "fcm_token": "<fcm token>"
    }
    ```

    On the other hand, if the device is a MacOS / iOS device, send in the APN device token:

    ``` 
    {
      "device_type": "android",
      "apn_token": "<fcm token>"
    }
    ```


### Notify Mac device when an Android device receives a SMS text

1. When the Android device receives an SMS text, it sends a request to the server (**POST** ```/api/v1/<device-id>/sms/messages/new```)

    ``` 
    {
      "phone_number": "<phone number>",
      "body": "<body>",
      "timestamp": "<timestamp>"
    }
    ```

2. The server receives the request, adds a new entry to the database

3. The Mac device receives a new event from the web socket

4. The Mac device makes a notification to the user

### Send sms text from my Mac to my Android phone

1. The Mac device makes a request to the server (**POST** ```/api/v1/<device-id>/sms/messages```):

    ```
    {
      "address": "<phone number>",
      "body": "<sms body>",
    }
    ```

2. The server receives the request, generates a random UUID, and sends this payload to the FCM:

    ``` 
    {
      "message": {
        "token": "<DEVICE TOKEN>",
        "data": {
          "device_origin_id": "<MAC DEVICE ID>",
          "device_dest_id": "<ANDROID DEVICE ID>",
          "action": "send_sms",
          "uuid": "<UUID>",
          "phone_number": "<phone number>",
          "body": "<sms body>",
        }
      }
    }
    ```

3. The server returns an OK response to the Mac device with this payload:

    ``` 
    {
      "status": "requested",
      "message-uuid": "<UUID>"
    }
    ```

4. The Mac device repeatedly polls the server on this endpoint: **GET** ```/api/v1/<device-id>/sms/messages/<UUID>/status```

5. The Android device receives the FCM request, and sends the SMS message

6. When the SMS message is sent successfully, the Android device sends an HTTP request to the server (**PUT** ```/api/v1/<device-id>/sms/messages/<UUID>/status```):

    ``` 
    {
      "status": "sent",
    }
    ```

7. The Mac device notices that the status changed from "requested" to "sent", and stops polling

### Getting a list of SMS conversations on the Android device from the Mac device

When getting SMS conversations from the Android device to the Mac device:

1. The Mac device makes an HTTP request to the server (**GET** ```/api/v1/<device-id>/sms/threads```)

2. The server receives the request, and sends this payload to FCM:

    ```
    {
      "message": {
        "token": "<DEVICE TOKEN>",
        "data": {
          "action": "update_sms_threads",
        }
      }
    }
    ```

3. The server also obtains the last list of SMS threads from the database, and returns the data to the Mac device

4. When the Android device receives the request, it obtains the SMS threads and makes an authenticated request to the server (**PUT** ```/api/v1/<device-id>/sms/threads```)

    ``` 
    {
      "sms-threads": [
        {
          "thread-id": "<THREAD ID>"
          "contact-name": "<CONTACT NAME>",
          "last-time-message-sent": "<TIMESTAMP>",
          "last-message-body-sent": "<TIMESTAMP>",
          "num-unread-messages": "<NUM_UNREAD_MESSAGES>",
        },
        ...
      ],
      "last-updated": <TIMESTAMP>
    }
    ```

5. The server receives the request, updates the database with the new content, and returns an OK response

### When getting SMS messages of a particular thread on an Android device in the Mac device:

1. The Mac device makes an HTTP request to the server (**GET** ```/api/v1/<device-id>/sms/threads/<thread-id>/messages```)

2. The server receives the request, and sends this payload to the FCM:

    ``` 
    {
      "message": {
        "token": "<DEVICE TOKEN>",
        "data": {
          "action": "update_sms_thread_messages",
          "thread_id": "<THREAD_ID>"
        }
      }
    }
    ```

3. The server also obtains the last list of messages of that thread from the database, and returns the data to the Mac device

4. The Android device receives the request, obtains the SMS messages for that thread, and makes an authenticated request to the server (**PUT** ```/api/v1/<device-id>/sms/threads/<thread-id>/messages```)

5. The server receives the request, authorizes the request, updates the database with new content, and returns an OK response
