# Introduction

This document is about how messages get sent back and forth to achieve the required features ([link](Design\ Document.md)). These features are:

1. Registering a device

2. Connecting, subscribing, and publishing a message to the MQTT broker

3. Pinging a device

4. Receive sms text from my Android phone to my Mac

5. Send sms text from my Mac to my Android phone

6. Getting a list of SMS conversations on the Android device from the Mac device

7. When getting SMS messages of a particular thread on an Android device in the Mac device:

8. Recieve notifications from my Android phone to my Mac

9. Reply to notifications from my Mac to my Android phone


# Background information

### MQTT:

* With MQTT, you can have devices publish a message to a topic. Subscribers are other devices that are listening to new messages to that topic.

### FCM:

* With FCM, you can send two types of messages asynchronously to Android devices:

    * Notification messages: are messages that get alerted to the user
    * Data messages: are messages that don't get alerted to the user

* Notification message format:

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

* Data message format:

    ``` 
    {
      "message": {
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

1. When the Android device wants to be registered to the service, it sends a request to the server (**POST** ```api/v1/devices/register```)

    ``` 
    {
      "device_type": "<device type>",
      "hardware_id": "<the hardware id>",
      "name": "<name of device>",
      "capabilities": [
        "<capability 1>",
        ...
      ]
    }
    ```

    where:
    * ```device_type``` is the type of device, like ```android``` and ```macbook```
    * ```hardware_id``` is the hardware id of the device
    * ```name``` is the name of the device, like ```My Android``` or ```My Mac```,
    * ```capabilities``` is a list of capabilities. The ones currently supported are:
        * ```ping_device``` - device has ability to be pinged
        * ```send_sms``` - device has ability to send sms
        * ```receive_sms``` - device has ability to receive incoming sms
        * ```read_sms``` - device has ability to read existing sms
        * ```read_contacts``` - device has ability to read contacts
        * ```receive_notifications``` - device has ability to receive incoming  notifications from other apps
        * ```respond_to_notifications``` - device has ability to respond to active notifications

### Updating the remote push notification token (FCM, APN, etc.) token:

1. When the device receives a new push notification token, it will send a request to the server (**POST** ```api/v1/devices/<device-id>/token```) with this payload:

    ```
    {
      "new_token": "<new token>"
    }
    ```

2. The server receives the request, updates the token from the database, and returns this message back to the device:

    ```
    {
      "status": "success"
    }
    ```

### Connecting the Mac / Android device to the MQTT broker:

1. First, the device establishes a connection to the MQTT broker

2. The device then sends a Connect message to the broker, setting the username field to the device's device id, and the password field to the OAuth token

3. The MQTT broker checks the device id against a database of registered devices (in Firestore); and the OAuth token against Firebase Auth service

4. When successful, the MQTT broker sends a [CONNACK](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718033) message, telling the device that the connection was authenticated

5. When unsuccessful, the MQTT broker sends a CONNACK message with a [failed return code](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Table_3.1_-), and disconnects the connection


### Publishing to a topic

1. Each topic a device wants to publish to must be prefixed with the ```device-id```

    * Example: the topic ```123/sms/messages/new``` has the device id ```123```

2. Once the Mac / Android device is connected to the MQTT broker, when it wants to publish a message to a topic, the MQTT broker will check if the device id in the topic exists. If not, it will return a PUBACK with a failed return code

3. When the device id from the topic exists, it will check if the device is registered under the same user as the publishing device. If not, it will return a PUBACK with a failed return code

4. If (2) and (3) passes, it will publish the message to that topic, and send a PUBACK message with an OK return code

5. The server also sends an empty remote push notification to the device via FCM or APN depending on what type the device is

    * Currently, it only sends empty remote push notifications to FCM when the device is an Android phone

### Subscribing to a topic

1. It is similar to publishing to a topic. Each topic a device wants to subscribe to must be prefixed with the ```device-id```

    * Example: the topic ```123/sms/messages/new``` has the device id ```123```

2. Once the Mac / Android device is connected to the MQTT broker, when it wants to publish a message to a topic, the MQTT broker will check if the device id in the topic exists. If not, it will return a [SUBACK](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718071) with a [failed return code](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Figure_3.26_-).

3. When the device id from the topic exists, it will check if the device is registered under the same user as the subscribing device. If not, it will return a SUBACK with a failed return code

4. If (2) and (3) passes, it will allow the device to subscribe to that topic by sending a SUBACK message with an OK return code

### Notify Mac device when an Android device receives a SMS text

1. The Mac device subscribes to the topic ```<device-id>/sms/new-messages``` where ```<device-id>``` is the device id of the Android device

2. When the Android device receives an SMS text, it publishes a message to the topic ```<device-id>/sms/new-messages```:

    ``` 
    {
      "phone_number": "<phone number>",
      "body": "<body>",
      "timestamp": "<timestamp>"
    }
    ```

3. The Mac device receives the message, and makes a notification to the user

### Pinging a device

1. The device to be pinged is already subscribed to the topic ```<device-id>/ping/requests``` where ```<device-id>``` is the device's device id to be pinged

2. Another device that will ping the device from (1) will publish a message to ```<device-id>/ping/requests``` with an empty payload:

    ```
    {}
    ```

### Send sms text from my Mac to my Android phone

1. The Android device is already subscribed to the topic ```<device-id>/sms/send-message-requests``` where ```<device-id>``` is the device id of the Android device

2. The Mac device publishes a message to the topic ```<device-id>/sms/send-message-requests```:

    ```
    {
      "phone_number": "<phone number>",
      "message": "<sms body>",
      "message_id": "random UUID",
    }
    ```

3. The Mac device subscribes to the topic ```<device-id>/sms/send-message-results``` where ```<device-id>`` is the device id of the Android device:

4. The Android device (which is already subscribed to ```<device-id>/sms/send-message-requests```) receives the message, sends the SMS message, and publishes the results to the topic ```<device-id>/sms/send-message-requests```:

    ``` 
    {
      "message_id": "UUID from (1)",
      "status": "<"success"/"delivered"/"failed">,
      "reason": "<reason>",
    }
    ```

5. The Mac device receives the message and decides whether to resend the message, etc.

### Getting a list of SMS conversations on the Android device from the Mac device

1. The Android device subscribes to the topic ```<device-id>/sms/threads/query-requests``` where ```<device-id>``` is the device id of the Android device

2. The Mac device subscribes to the topic ```<device-id>/sms/threads/query-results``` where ```<device-id>``` is the device id of the Android device from (1)

3. The Mac device publishes a message to ```<device-id>/sms/threads/query-requests``` with this payload:

    ```
    {
      "limit": <limit>,
      "start": <start>
    }
    ```

    where:
    
    * ```limit``` is the max. number of threads to fetch
    * ```start``` is the starting index of the thread to fetch

4. The Android device receives that message, performs the query, and publishes the results to the topic ```<device-id>/sms/messages/query-results``` with this payload:

    ```
    {
      "limit": "<limit from (1)>",
      "start": "<start from (1)>",
      "threads": [
        {
          "thread_id": "<thread-id>",
          "phone_number": "<the recipient's phone number of this thread>",
          "contact_name": "<the recipient's contact name of this thread>",
          "num_unread_messages": <the number of unread messages>,
          "num_messages": <the number of messages in this thread>,
          "last_message_sent": "<the last message in this thread>,
          "time_last_message_sent": "<the time the last message was sent / received in unix time>"
        },
        ...
      ]
    }
    ```

5. The Mac device pattern-matches the  ```limit```, and ```start``` fields of incoming messages from ```<device-id>/sms/threads/query-results``` against the message it sent from (3), receives the results, and displays the results to the user

### When getting SMS messages of a particular thread on an Android device in the Mac device:

1. The Android device subscribes to the topic ```<device-id>/sms/messages/query-requests``` where ```<device-id>``` is the device id of the Android device

2. The Mac device subscribes to the topic ```<device-id>/sms/messages/query-results``` where ```<device-id>``` is the device id of the Android device from (1)

3. The Mac device publishes a message to the topic ```<device-id>/sms/messages/query-requests``` with this payload:

    ```
    {
      "thread_id": "<thread-id>",
      "limit": <limit>,
      "start": <start>
    }
    ```

    where:
    
    * ```thread_id``` is the ID of the SMS thread
    * ```limit``` is the max. number of messages to fetch
    * ```start``` is the starting index of the message to fetch

4. The Android device receives that message, performs the query, and publishes the results to the topic ```<device-id>/sms/messages/query-results``` with this payload:

    ```
    {
      "thread_id": "<thread id from (1)>",
      "limit": "<limit from (1)>",
      "start": "<start from (1)>",
      "messages": [
        {
          "messageId": "<message id>",
          "phone_number": "<the phone number>",
          "person": "<the contact name behind the phone number>",
          "body": "<the sms body>",
          "read_state": <true if it was read; else false>,
          "time": <time>,
          "type": "inbox" / "sent"
        },
        ...
      ]
    }
    ```

    where:

    * ```type```: "inbox" if it was sent by someone; else "sent" if it was sent by the Android device
    * ```time```: unix time it was sent / received in milliseconds

5. The Mac device pattern-matches the ```thread_id```, ```limit```, and ```start``` fields of incoming messages from ```<device-id>/sms/messages/query-results``` against the message it sent from (3), receives the results, and displays the results to the user

### Notify Mac device when an Android device receives a notification from another app

1. The Mac device subscribes to topic ```<device-id>/notification/new``` where ```<device-id>``` is the device id of the Android device

2. When the Android device receives a notification from another app, it publishes a message to ```<device-id>/notification/new``` with the payload:

    ```
    {
      "id": "<the notification id>",
      "app": "<the name of the app>",
      "time_posted": "<the time it was received>",
      "title": "<the title of the notification>",
      "text": "<the content text of the notification>",
      "actions": [
        {
          "type": "action_button" / "direct_reply_action",
          "text": "<the name of the action>"
        }
      ]
    }
    ```

    where:

    * ```action_button``` refers to buttons that indicate a one-time action [(link)](https://developer.android.com/training/notify-user/build-notification#Actions)
    * ```direct_reply_action``` refers to actions with a textbox [(link)](https://developer.android.com/training/notify-user/build-notification#reply-action)

3. The Mac device receives the notification, and dispatches a notification to the user


### Responding to a notification on the Mac device that was forwarded from an Android device

1. The Android device was already subscribed to the topic ```<device-id>/notification/responses``` where ```<device-id>``` is the device id of the Android device

2. The Mac device publishes a message to ```<device-id>/notification/responses``` with the payload:

    ```
    {
      "key": "<the notification id>",
      "action_type": "action_button" / "direct_reply_action",
      "action_title": "the action title",
      "action_reply_message": "<the reply if the type is 'direct_reply_action'; else null>"
    }
    ```

3. The Android device receives the message, and responds to the notification


